# RootHelper
An extension to use libsuperuser library more easily

Requirements
------------
MinAPI is 9, and the device has to be rooted in order to perform root operations (obviously...).

How to use
----------
You have a global singleton Root:

    Root root=Root.getInstance();

With it, you first have to gain root privilege from the user, and might take some time. You have 2 options for gaining root privilege:

    public boolean getRoot()
    or    
    public void getRoot(final IGotRootListener listener)

The first can only be used in background thread, and the other can be called on the UI thread.

After you got root privilege, you can perform root operations, using any of those :

    public List<String> runCommands(@NonNull final List<String> commands)
    or
    public List<String> runCommands(@NonNull final String... commands)

You give them a list of commands to run, and they will return you a list of the output from the commands (or null in case of any error, like missing root privilege).

Example
-------
If you have Android API 11 and above, this will show the recent tasks :

    new Thread(){
        @Override
        public void run() {
            final Root root = Root.getInstance();
            final boolean gotRoot = root.getRoot();
            if(gotRoot)
                root.runCommands("input keyevent " + KeyEvent.KEYCODE_APP_SWITCH);
        }
    }.start();

The sample in this project will just show that the count of files in a protected path is positive for root operations, yet 0 for normal API. 
