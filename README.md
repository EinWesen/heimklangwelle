# Heimklangwelle - A Basic DNLA®️ hub

This project aims to be a simple all-in-one solution to share (audio) media locally in your lan by providing 
a renderer (a player) to play media on, a very simple controlpoint that enables you to select files to play (via web),  lastly a media server as well.

The goal is to keep it simple in every way. Not only only easy to use, but also only basic in functionality.
At the moment i do not intend to implement everything the DNLA®️ standard has to offer, or advanced feature like full blown media libraries or on the fly transcoding. It's the app you fire up, when you need to stream you files real quick once in a while.

## But why? - Use case
I wanted to play some music on a bluetooth speaker from my PC, but it does not have a bluetooth chip.
What i do have however is a RasberryPI, that does have one. Then i had the idea to use a commandline-player on there to send it to my bluetooth speaker. Having remembered the DNLA®️ from years back when i used it for video stuff, i first thought there surely should be some project that just provide a simple renderer, but no. I did not find a simple one, and now here we are.

Is this best way to solve that problem? - Of course not! There are so many easier ways, to do this. Ranging from copying the files on the rasberry, using my phone instead, or just simply buying a cheap bluetooth dongle. Not to mention that most people just don't really use local media anymore in the first place. Including me. But it seemed liek a fun little project.

## Features / TODO
- [x] Basic infrastruture
    - [x] Implement UPNP Services/Discovery 
    - [ ] Provide basic Webserver for Controlpoint WebUI
- [x] Mediarenderer
    - [x] Implement transport/controller service infrastruture
    - [x] Implement the actual renderer based on ~~mplayer~~ mpv
    - [x] Enhance the IPC class to work on linux ( = posix?)
- [ ] Controlpoint
    - [ ] Implement backend to handle UPNP-communication with mediaservers
    - [ ] Implement WebPlayer UI
- [x] Mediaserver
    - [x] Implement mediaserver service infrastruture
    - [x] Implement the actual contentlisting (on disk) and serving

## Acknowlegements
This would not be possible without these: 
- https://www.jupnp.org/ | https://github.com/jupnp/jupnp 
- https://mpv.io/ | https://github.com/mpv-player/mpv

## Other good stuff
The [Serviio](https://serviio.org) media server has worked really well for me in he past.

[eezUPnP](https://www.eezupnp.de) is not only a really good controler, that allowed me to use my renderer as soon as it was ready, nut as also immensly helpful during developement of the same (and the media server as well).
