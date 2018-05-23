#### Minecraft forge mod chopdown brings realistic tree chopping that makes logs and leaves fall down after cutting
### Updated version handles trees in forests, really large trees, even the natura redwoods and drops trees more realistically.
![](logo.png)

Features
+ Falldown opposite direction along the axis (Diagonal fall was removed because you can't have the tree fall diagonally and look as realistic)
+ Multi threaded capability meaning even for really large trees it will not cause you an issue in multiplayer.
+ Handles any size tree including naturas redwoods (though there is a delay the server is still responsive while it calculates the fall)
+ No dependencies
+ Different trees should not get mixed up, got a birch next to an oak tree, chop down the birch and only the birch tree and leaves will fall.
+ In same type forests it does its best to calculate which leaves and branches should belong to the tree you are chopping
+ You must chop all the way through the trunk to fell the tree, not just 1 block
+ Server only allows a player to chop 1 tree at a time down, you could not spam for example serveral redwoods at once (though achieving this would be hard in the first place it cuts all chance for abuse)
+ Trees can be felled from any point on the trunk
+ Trees still do the fell in creative, want to make your creative world look more realistic just go chop some random trees down.
+ Highly configurable.

Installation
+ Download jar for your version
[1.12.2+](https://github.com/Shovinus/ChopDownUpdated/raw/master/versions/1.12.2/bin/ChopDown%20Updated%20(1.12.2%2B)-1.0.10.jar)
[1.9-1.11.2](https://github.com/Shovinus/ChopDownUpdated/raw/master/versions/1.9-1.11.2/bin/ChopDown%20Updated%20(1.9-1.11.2)-1.0.10.jar)
+ Put the jar to your minecraft mods folder

This mod is not really like the first, the first by Ternsip was a very cool idea and very simple, however while using it there were a few issues I found, for example ternsips version uses a 3d radius as apposed to horizontal, meaning that large jungle and spruce trees end up with their tops floating in mid air.
Then there was the case that chopping a tree in a forest would drag all connected trees over.

Also it would actually just shove all the blocks 1 block over for every block up, meaning that you could end up with for example large oak trees, the leaves falling on you.

I have tried to solve these issues as best as possible and make it as immersive as possible so it feels like you are chopping a tree down.
There are of course still flaws that cannot really be solved. Such as Trees falling onto other trees looking wierd. There is no real way around this.

You can also configure this mod to help with lower powered servers.

FORKED BY SHOVINUS
