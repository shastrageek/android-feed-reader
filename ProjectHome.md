# Overview #
AFR is yet another implementation of a RSS/Atom feed reader for the Android platform. It is functional with regards to reading feeds, but otherwise very rough around the edges.

Currently, it uses the ROME + JDOM libraries for parsing feeds, and the java.beans module from Apache Harmony, which is required by ROME.

# Status #
Current version is 0.1.  Currently, this project is mainly just an exploration of the Android SDK.

## Done ##
  * basic feed reader functionality
  * basic UI
## Todo ##
  * favicons
  * categories
  * configurable automatic polling
  * figure out if it's possible to add an intent to the browser for rss/atom links
  * fix the many many bugs that are probably hiding

# Screenshots #
|![http://telusplanet.net/~gilbertr/afr/feeds.png](http://telusplanet.net/~gilbertr/afr/feeds.png)|![http://telusplanet.net/~gilbertr/afr/notification.png](http://telusplanet.net/~gilbertr/afr/notification.png)|
|:------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------|
|![http://telusplanet.net/~gilbertr/afr/entries-date-open.png](http://telusplanet.net/~gilbertr/afr/entries-date-open.png)|![http://telusplanet.net/~gilbertr/afr/entries-date-collapsed.png](http://telusplanet.net/~gilbertr/afr/entries-date-collapsed.png)|![http://telusplanet.net/~gilbertr/afr/entries-authors.png](http://telusplanet.net/~gilbertr/afr/entries-authors.png)|