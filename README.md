# Common-utils

A collection of utility classes that can help when writing standalone Java applications.

These classes address the following needs:

### Logging and message handling

In the com.threeamigos.common.util.interfaces.messagehandler package there are some functional interfaces
that can be used to handle info, warn, error, debug or trace messages, and exceptions.
Implementations for these classes include a console logger, an in-memory store (useful to run tests),
a popup dialog, and a forwarder used to route messages to one or more other handlers.
For each of these handlers, you can enable or disable a given level of messages. Thus, you can disable debug
or trace messages if you want to run your application in production mode. Or, using a CompositeMessageHandler,
you can forward a certain level of messages to a log file while sending other messages to the user via a popup window.

The common class for those handlers is the AbstractMessageHandler, from which you can enable or disable certain
message levels and derive your own handlers.

You can replace the handler with a custom one while keeping the rest of your code unchanged. Should not be too
difficult to e.g., implement a handler that sends messages to a Slack channel or to a Log4J appender.

Handlers accept messages or Suppliers of messages, which can be useful for lazy evaluation of messages.

### JSON conversion

Besides web applications, JSON can be used to store preferences and other data. Based on Google's Gson library,
the Json interface provides a simple way to convert from/to JSON strings and Input/OutputStreams.

Use the JsonBuilderFactory to build a Json converter.

Adapters can be defined to convert between Java objects and JSON objects, such as java.awt.Color (provided).
The JsonBuilder can register one or more adapters and build a Json converter for a certain class. It is
basically a simplification for the Gson library.

### Persistence

Most of the time you will need to store some data somewhere. The Persistable functional interface asks an object
to save its state somehow. A PersistablesCollector can be used to collect a set of Persistables. When the persist()
method is called on a PersistablesCollector, it will ask all the Persistables it tracks to persist their state,
without having to remember all of them manually.

A BasicPersistablesCollector is provided, which asks all tracked Persistables to save their state when the
application exits, via a shutdown hook.

A more specific implementation, the JsonFilePreferencesCollector, stores a set of Preferences in JSON files so
that when the user launches the application again, the state of the Preferences is restored.

A PersistResult tells whether the operation was successful or not. It is a simplification of a load or save
operation, telling just if it succeeded or not (and eventually returns an error message).

The StatusTracker class keeps track of whether an object changed its status or not. The provided implementation
internally uses a Json converter to store the state and compare it with the previous one. A StatusTrackerFactory
builds StatusTracker objects. Should you persist the state of an object to a file, the FilePersistResult returns
the name of the file where the state was stored.

The RootPathProvider interface provides a way to get the root path of the application in which all files are stored.
Its default behavior is to use a directory in the user's home folder, using the package of the application's
main class as a directory in which the application files will be stored. The directory can, however, be specified
using the -Droot_path_directory=... command line argument.

### Preferences

Your application may contain preferences that you want to store and retrieve. For example, the location and size of
a window. The Preferences interface provides a way to store and retrieve preferences. It can check if a given set
of values is valid, and if it is not, it can ask the tracked objects to fix it. Some basic preferences are provided,
such as Window preferences, which can be used to store the location and size of a window, Main and Secondary Window
preferences, and Hints preferences.

#### Putting it all together

Basically, you can have a set of Preferences that drive various parts of your application. At startup, the
application can check if the preferences are valid, and if they are not, it can ask the tracked objects to fix them
returning to a valid, predefined state.
This way, the application can ensure that the preferences are always valid and that the user is not presented with
invalid or incomplete preferences.

When the program exits, the JsonFilePreferencesCollector checks (via a JsonStatusTracker) whether the various
Preferences have changed or not, and modified preferences are saved.

So you need to:
- create a RootPathProvider (use the default one, RootPathProviderImpl)
- define a set of Preferences that you want to store and retrieve
- pass them to the JsonFilePreferencesCollector along with a StatusTrackerFactory, a MessageHandler, and a Json that
is able to do the conversion between JSON and your Preferences objects

That's it!

### Fonts

A standalone application may need to use fonts. The FontService acts as a simple cache for fonts.

### Hints

Hints are used to provide additional information to the user. The Hint interface represents one such hint.
A HintsProducer represents a part of the application that can provide hints for its functionality. As an application
can be composed of multiple parts, each part can provide hints, and a HintsCollector can be used to collect all the
hints from all the parts. A HintsDisplayer can display the hints collected by a HintsCollector. A simple
implementation (HintsWindowImpl) is provided that can go back and forth between all hints and remember if they are
to be shown at startup or not, tracking which one was the last shown.

### Input handling

Standalone Java applications often need to handle user input. The InputConsumer interface handles keyboard and mouse
events. The ChainedInputConsumer is an implementation that can forward events to one or more InputConsumers until
the event is consumed by one of them. In this way different parts of the application can provide their own
InputConsumer to the unique ChainedInputConsumer, thus isolating event handling.

### Misc

A simple About Window is provided, with a way to show the version of the application and the author. A custom image
can be used.

Support for drag-and-drop is provided to pass a list of files to an application.

### Disclaimer

Code is provided as-is, without any guarantee of correctness or suitability for any particular purpose.
Those classes were written for my own pet projects, Mandelbrot and PixelPeeper. Mostly they are used by PixelPeeper.

Revision history:

1.0.2:
- first initial release

1.0.3:
- some refactoring

1.0.4:
- added support for lazy evaluation of messages
- added support for localization
- added JSpecify annotations
- documentation improvements
- added JsonBuilderFactory
- added registerColorAdapter to JsonBuilder interface
- handlers now support debug and trace messages
- 100% test coverage
- exceptions are thrown when null objects are passed as NonNull parameters
