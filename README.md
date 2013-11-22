find . -name \*jetty\*.jar -print0 | xargs -0 -t -n 1 -J POOP zip -d POOP about.html

