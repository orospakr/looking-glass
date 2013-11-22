## Looking Glass

Android's Content Provider interface embodies so much of the same
principles as RESTful HTTP.

This app exposes your apps' Content Providers as easily callable REST
APIs.  All this is done on the phone; no external service is
necessary.

Try using some of your phone's providers via your favourite REST
client library, like ActiveResource.

I also intend to add some useful web-based tools for some common
content providers in order to make a conveniently human usable web UI
for some of Android's data services.

(Not to be confused with an Internet routing "Looking Glass".)

### License

Copyright (C) 2013 Andrew Clunis <andrew@orospakr.ca>

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License.  The
license is included with this project as "LICENSE".

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.

### Building

Warning!  Before you continue, this app is fairly dangerous to leave
on your phone until I implement the security features.

Build the project:

    $ gradle assemble
    
Install on your phone:

    $ gradle installDebug
    
Get JSON of your browser bookmarks (first, launch the app's activity):

    $ curl http://phone-ip:4567/cp/browser/bookmarks

You may find that, until a fix that I proposed gets into the Android
gradle build system's upstream, you'll need to remove all the
informative `about.html` files from the Jetty jars downloaded as
dependencies in order to avoid a duplicate file error.  In that case,
try this hack (deletes about.html from all Jetty jars in your Gradle
cache):

    $ cd .gradle/caches
    $ find . -name \*jetty\*.jar -print0 | xargs -0 -t -n 1 -J POOP zip -d POOP about.html

## Tasks

* Add all the permissions for accessing the built-in content
  providers;
* Token-based pairing of HTTP clients via interactive prompts on the
  device;
* See about any hacks applicable to rooted devices to enable access to
  *all* content providers regardless of permissions;
* Built-in human-useable JS clients for some of the interesting
  built-in providers.  SMS comes to mind.
* Various TODOs spread in the code.
