// **********************************************************************
//
// Copyright (c) 2003-2018 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

public class Server
{
    static class ShutdownHook extends Thread
    {
        @Override
        public void
        run()
        {
            _communicator.destroy();
        }

        ShutdownHook(Ice.Communicator communicator)
        {
            _communicator = communicator;
        }

        private final Ice.Communicator _communicator;
    }

    static class SenderShutdownHook extends Thread
    {
        @Override
        public void
        run()
        {
            _sender.destroy();
            try
            {
                _senderThread.join();
            }
            catch(InterruptedException e)
            {
            }
        }

        SenderShutdownHook(CallbackSenderI sender, Thread senderThread)
        {
            _sender = sender;
            _senderThread = senderThread;
        }

        private final CallbackSenderI _sender;
        private final Thread _senderThread;
    }

    public static void
    main(String[] args)
    {
        int status = 0;
        Ice.StringSeqHolder argsHolder = new Ice.StringSeqHolder(args);

        //
        // Try with resources block - communicator is automatically destroyed
        // at the end of this try block
        //
        try(Ice.Communicator communicator = Ice.Util.initialize(argsHolder, "config.server"))
        {
            //
            // Install shutdown hook to (also) destroy communicator during JVM shutdown.
            // This ensures the communicator gets destroyed when the user interrupts the application with Ctrl-C.
            //
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(communicator));

            if(argsHolder.value.length > 0)
            {
                System.err.println("too many arguments");
                status = 1;
            }
            else
            {
                Ice.ObjectAdapter adapter = communicator.createObjectAdapter("Callback.Server");
                CallbackSenderI sender = new CallbackSenderI(communicator);
                adapter.add(sender, Ice.Util.stringToIdentity("sender"));
                adapter.activate();

                Thread t = new Thread(sender);
                t.start();

                //
                // Add second shutdown hook to destroy sender
                //
                Runtime.getRuntime().addShutdownHook(new SenderShutdownHook(sender, t));

                communicator.waitForShutdown();
            }
        }

        System.exit(status);
    }
}
