Copyright (c) 2016 - 2017, George Varghese M.

(Note: This project is currently unlicensed and NOT open sourced. You do not have permission to reproduce or publish the source code or the application anywhere. However, I am planning to make it open sourced once I reach a certain level of progress, perhaps, in the next few months. So stick around)
# GlidePlayer
An android app that should make locally sharing music easier.

The aim of this project is to create an android app that can allow nearby GlidePlayer app users to connect with each other in a local "GlidePlayer" group, inside which, each user can see each other's music libraries and be able to play files from it seamlessly. No need to "beam" files between devices, no need to stream audio. The app pulls in audio files as and when needed. 

Want to get your friends to check out that new album you just got? Just open the app, connect and play! (As long as you're within range of each other)

##### Quick note
This project is far from done. It's still in it's infancy but every step so far has been fun and I've learned so much that it's been worth every day spent on it. I really hope I get the time to keep working on it until I end up with acceptable results. So I've got my fingers crossed and seatbelt buckled in! (that sounded better in my head)

### About the project
This project is both a way of implementing an idea of mine and also to learn Android App development from scratch. Progress is slow and steady as I learn to write better code and use the Android API efficiently, all while getting to see an idea of mine possibly turn into a reality.

### Planned features
* Allow nearby users to create a group
* Group members can see each other's music library and play any song from it seamlessly and only as long as the group is active.
* Songs from another device are pulled and cached when needed. No need to stream constantly.
* A "synchronized" play feature where members of the group can share one common play queue. Everyone can contribute to the play queue and hitting play, pause, skip will do the same thing on everyone else's device.
* More possible features to take shape in the future.


### Current progress: 
##### Beta Version 0.1 Released
You can download the app from Google Play Store at this link: https://play.google.com/store/apps/details?id=com.teefourteen.glideplayer or you can download the apk from the release section of the GitHub project.

#### What the app can do so far
* Play local songs like a simple music player
* Connect to other devices over a Wifi-Direct group
* Two and more devices running the app and joining a WifiDirect network together can share their music libraries with each other and play any song from each other's device. The song isn't buffered though, it starts playing only once the entire song file has been downloaded, which can take a while
