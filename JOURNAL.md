# Alfred: Personal Assistant [for Android]

(aka: "If Mark Zuckerberg can attempt to do it, then why can't I?")
(answer: probably because he has billions of dollars and hundreds of
 assistants.)

### 2017/04/14:

Alfred shipped to Google Play this week, and I spent the rest of it cleaning
up the build scripts and automating it nicely. Every branch gets built now
as assembleRelease (proguarded [since that often breaks], but not signed),
and all I have to do to automatically push to Google Play is create a Release
on GitHub and the Travis CI will take care of the rest (sign, upload APK
to Google Play and GitHub and mapping.txt to GooglePlay, Firebase, and GitHub).

I have a normal job that I need to concentrate on for a few weeks, and during
that time I am going to work on some non-coding tasks such as finishing my
11'x4' whiteboard wall at home and organizing Alfred's next tasks.

I'll be back working on Alfred soon after I bang out a strategy and tactics
on how to do what needs to be done next.

### 2017/04/06:

Alfred is stable and functional, but I am not comfortable releasing him
until I finish code to disable him while in a phone call. This is a
blocking issue that prevents the app from being useful in the
real-world.

### 2017/03/31:

I've been working fairly solid on this project since early March. A lot
of emphasis has been put on the startup and detection of proper
enabled/disabled state. This has resulted in a decently solid code base
that with a bit more work can maintain a running pseudo-AI fed by
internal state logic and parsed notifications.

I also had a nifty vision of how Alfred could be made a more usable and
unique general purpose app; Top secret for now, and that will probably
be released as a separate app, but I'll be tinkering with this.

Speech-To-Text has been put on hold and will have to wait until other
higher priority features are implemented.

### 2016/05/22:

Things are progressing slowly, but they are progressing. Currently, my
[SmartFoo](http://github.com/SmartFoo/smartfoo) library is working well,
and Alfred is able to generically parse and speak any notification, and there
are custom parsers for several specific app notifications (Pandora, Spotify,
etc).

My general plan is this:

1. Get Text-To-Speech working reliably. 
    1. Customize notification parsers to make all read text coherent.  
       The only current issues I am aware of are:  
        1. Phone numbers are spoken as "4 billion 258 million 914 thousand 796".
        2. Every determinate progress indicator increment, such as used by Google Play Updates, is spoken.
        2. Attenuation get stuck on when multiple texts are spoken. 
2. Get app state and usability working intuitively.
3. Start to work on Speech-To-Text.

### 2016/01/10:

On 2016/01/03 MZ announced (via https://www.facebook.com/zuck/posts/10102577175875681):  
"My personal challenge for 2016 is to build a simple AI to run my home and help me with my work. You can think of it kind of like Jarvis in Iron Man."

Meh! There seems to be very little specifics here, and I am skeptical that his effort(s)/progress will be publicized much along the way.

I've had an itch to do this for years, and in 2014/09 I [very] briefly started something by forking pocketsphinx:

* https://github.com/swooby/pocketsphinx-android
* https://github.com/swooby/pocketsphinx-android-demo

Personal issues [some of the worst I have ever been through] quickly interrupted those plans.  
Those plans were to take place along several fronts:

1. Obviously, "The Actual End Product App" that everyone sees
2. The hardware needed to make TAEPA work
3. The supporting software libraries to make TAEPA work

I've been working on these subconsciously over the past few years,  
and the past year has been a stealth attempt for tangibles on #2 and #3.

This repo is my new revived attempt at trying this again, and I guess it can't hurt to use MZ as inspiration.  
Heck, maybe I can follow along w/ him and learn something.  
Heck, maybe he will find this project and learn from from me! :)
