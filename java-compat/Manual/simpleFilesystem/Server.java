// **********************************************************************
//
// Copyright (c) 2003-2018 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

import Filesystem.*;

public class Server
{
    static class ShutdownHook extends Thread
    {
        @Override
        public void
        run()
        {
            //
            // Destroy returns only when the communicator is fully destroyed
            //
            _communicator.destroy();
            System.err.println("terminating");
        }

        ShutdownHook(Ice.Communicator communicator)
        {
            _communicator = communicator;
        }

        private final Ice.Communicator _communicator;
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
        try(Ice.Communicator communicator = Ice.Util.initialize(argsHolder))
        {
            //
            // Install shutdown hook to destroy communicator during JVM shutdown,
            // including when the user interrupts the application with Ctrl-C
            //
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(communicator));

            status = run(communicator, argsHolder.value);
        }

        System.exit(status);
    }

    private static int
    run(Ice.Communicator communicator, String[] args)
    {
        //
        // Create an object adapter.
        //
        Ice.ObjectAdapter adapter =
            communicator.createObjectAdapterWithEndpoints("SimpleFilesystem", "default -h localhost -p 10000");

        //
        // Create the root directory (with name "/" and no parent)
        //
        DirectoryI root = new DirectoryI(communicator, "/", null);
        root.activate(adapter);

        //
        // Create a file called "README" in the root directory
        //
        FileI file = new FileI(communicator, "README", root);
        String[] text;
        text = new String[]{ "This file system contains a collection of poetry." };
        try
        {
            file.write(text, null);
        }
        catch(GenericError e)
        {
            System.err.println(e.reason);
        }
        file.activate(adapter);

        //
        // Create a directory called "Coleridge" in the root directory
        //
        DirectoryI coleridge = new DirectoryI(communicator, "Coleridge", root);
        coleridge.activate(adapter);

        //
        // Create a file called "Kubla_Khan" in the Coleridge directory
        //
        file = new FileI(communicator, "Kubla_Khan", coleridge);
        text = new String[]{ "In Xanadu did Kubla Khan",
                             "A stately pleasure-dome decree:",
                             "Where Alph, the sacred river, ran",
                             "Through caverns measureless to man",
                             "Down to a sunless sea." };
        try
        {
            file.write(text, null);
        }
        catch(GenericError e)
        {
            System.err.println(e.reason);
        }
        file.activate(adapter);

        //
        // All objects are created, allow client requests now
        //
        adapter.activate();

        //
        // Wait until we are done
        //
        communicator.waitForShutdown();

        return 0;
    }
}
