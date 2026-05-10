# Heimklangwelle - A Basic DLNA®️ hub

This project aims to be a simple all-in-one solution to share (audio) media locally in your LAN by providing 
a renderer (a player) to play media on, a very simple control point that enables you to select files to play (via web),  lastly a media server as well.

The goal is to keep it simple in every way. Not only easy to use, but also only basic in functionality.
At the moment I do not intend to implement everything the DLNA®️ standard has to offer, or advanced features like full blown media libraries or on the fly transcoding. It's the app you fire up, when you need to stream your files real quick once in a while.

## But why? - Use case
I wanted to play some music on a bluetooth speaker from my PC, but it does not have a bluetooth chip.
What I do have however is a RasberryPI, that does have one. Then I had the idea to use a commandline-player on there to send it to my bluetooth speaker. Having remembered the DLNA®️ from years back when I used it for video stuff, I first thought there surely should be some project that just provides a simple renderer, but no. I did not find a simple one, and now here we are.

Is this the best way to solve that problem? - Of course not! There are so many easier ways, to do this. Ranging from copying the files to the rasberry, using my phone instead, or just simply buying a cheap bluetooth dongle. Not to mention that most people just don't really use local media anymore in the first place. Including me. But it seemed like a fun little project.

## (Missing) Features
### MediaServer
- ✓ Serves media files from all drives, with the ability to browse
- ✓ Serves a set of fixed formats, decided by file extension
- ✗ No security at all
- ✗ No metadata support (filename is title)
- ✗ No transcoding support
- ✗ No search, just browsing
- ✗ No DLNA profiles associated with media (depends on the controller / renderer whether this is a problem)
- ✗ May return empty folders (no recursive checking for available files)
  
### Mediarenderer
- ✓ Can in theory play any media MPV can play (but advertises only some)
- ✓ Supports "RelativeTimePosition"
- ✓ Supports M3U(8) as long as they include reachable urls
- ✓ Support for Play,Stop,Pause and Next / Previous (for playlists)
- ✓ Supports a playlist queue as a custom/vendor function
- ✗ No support for setNextTransportURI / gapless playback
- ✗ No support for track duration or media duration
- ✗ No time or byte based seeking 
- ✗ No repeat or shuffle
  
### Mediacontroller
- ✓ Simple web based gui
- ✓ Control remote remote renderers or simple local player in browser
- ✓ Support for Play,Stop,Mute,SetVolume,Next,Previous
- ✓ Supports a playlist queue with NextMedia/PreviousMedia and auto advance
- ✓ Supports remote playlist queue as a custom/vendor function in conjunction with own renderer (Controller does not need to stay open)
- ✓ Displays track artist & title (if metadata is available)
- ✓ Displays playback time (synced by "RelativeTimePosition", if available
- ✗ No full metadata support (apart from the mentioned above)
- ✗ No support for setNextTransportURI / gapless playback
- ✗ Does not check for device capabilities
- ✗ Does not check for supported filetypes by renderer (just tries to play)

## Should you use this ?
Maybe (not). While the current state is totally usable in my opinion, and supports even more features than initially planned , it is bare bones in terms of UX (especially installation).
And since this was more of a educational / recreational project and does already more than I need, it is unlikely that it will see many updates.
I may fix bugs if I find them, but don't expect new features any time soon. In the same way, some dependencies are not even up to date at the time of writing.

## Acknowledgements
This would not be possible without these: 
- https://www.jupnp.org/ | https://github.com/jupnp/jupnp 
- https://mpv.io/ | https://github.com/mpv-player/mpv

## Other good stuff
The [Serviio](https://serviio.org) media server has worked really well for me in the past.

[eezUPnP](https://www.eezupnp.de) is not only a really good controller, that allowed me to use my renderer as soon as it was ready, but was also immensely helpful during development of the same (and the media server as well).
