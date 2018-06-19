[![Release](https://img.shields.io/github/release/AndroidDeveloperLB/RootHelper.svg?style=flat)](https://jitpack.io/#AndroidDeveloperLB/RootHelper)

# RootHelper
An extension to use libsuperuser library more easily

Requirements
------------
MinAPI is 9, and the device has to be rooted in order to perform root operations (obviously...).

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

And the result:

![enter image description here](https://raw.githubusercontent.com/AndroidDeveloperLB/RootHelper/master/extras/demo.gif)

The sample in this project will just show that the count of files in a protected path is positive for root operations, yet 0 for normal API. 

Note that it's adviced to show the user some kind of progress bar while getting the root privilege, because on some ROMs, it could take some time till the user sees a dialog asking if it's ok to grant your app root privilege.

How to import via gradle
------------------------
Import in gradle using :

root gradle file:

	allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}

your app's gradle file:

	dependencies {
	        implementation 'com.github.AndroidDeveloperLB:RootHelper:#'
	}

where "#" is the latest release of the library, as shown on [Jitpack's website](https://jitpack.io/#AndroidDeveloperLB/RootHelper/).


How to use
----------
You have a global singleton Root:

    Root root=Root.getInstance();

With it, you first have to gain root privilege from the user, and might take some time. You have 2 options for gaining root privilege:

    public boolean getRoot()
    or    
    public void getRoot(final IGotRootListener listener)

The first can only be used in background thread, and the other can be called on the UI thread.

After you got root privilege, you can perform root operations (only on background thread), using any of those :

    public List<String> runCommands(@NonNull final List<String> commands)
    or
    public List<String> runCommands(@NonNull final String... commands)

You give them a list of commands to run, and they will return you a list of the output from the commands (or null in case of any error, like missing root privilege).

