# Alfred: Personal Assistant [for Android]

### 2025/03/11 AM:

I have 2025 modernized M1:  
https://github.com/swooby/alfred.2017/tree/m1-2025-modernize

The coversion was pretty easy, espcially focusing on the diffs between `last-pre-2025-modernize` and [modernized] `M2`:  
https://github.com/swooby/alfred.2017/compare/last-pre-2025-modernize...m2?w=1

I merged that into M2 and can now compare them:  
https://github.com/swooby/alfred.2017/compare/m1-2025-modernize...m2?w=1

### 2025/03/10:

**8 YEARS?!?!?!***

It has been 8 years since I last updated this code!

Obviously a lot has changed (and nothing has changed).

The main reason I am revisiting this circa 2017 project is because of https://github.com/swooby/AlfredAI,
an AI based project obviously inspired by **this** project. 

AI became a mortal reality around 2022 and has taken off in 2024; some think that AGI could be reached in 2025 (I am skeptical).

In 2024/12 I was let go from my job (https://swooby.com/pv/resume/) and while unemployed I mashed up
my Push To Talk experience with my own personal version of OpenAI's Voice Assistant.

The [current] problem with this Alfred project was/is that there isn't any AI in it.
The [current] problem with the AlfredAI project is that it can't do much.

I loved using Alfred back in 2017; it's most powerful and probably only read feature was reading Notification.

I found this very useful when it worked, playing music or reading texts from my wife.

I always wanted to log these to a personal database that tracked what I did and then use AI to... do something.

I guess this would be for personal historical pseudo-autobiographical reasons.

What was I listening to when ...  
What was that song that was playing when ...  
How many gallons of milk did my wife tell me to pick up at the store? 

I am rambling... but this is a Journal.

Point is... this Alfred project is back for a little while; at least until I incorporate its good
parts into https://github.com/swooby/AlfredAI.

I spent yesterday getting the project to rebuild in 2025 Android Studio Meerkat with mostly current
dependencies.

Technical note to self (because I have forgotten some of the state of this project):
* The M2 branch is the default branch because, as you can infer from the
  https://github.com/swooby/alfred.2017/blob/m2/README.md#milestones section, I deemed that Alfred
  had reached basic M1 functionality and had moved on to start working on M2 functionality.
* I don't remember how far I got into getting the M2 functionality working. I see bits and pieces
  of code that line up with the stated milestone features, but the code smells like it is 
  incomplete, and the fact that I never moved on to M3 I consider proof that the existing code was
  left in an incomplete and probably/most-likely non-functional state.
* Considering this, it may be useful to go back to where M2 was branched off of master to pick up  
  any working last known good bits.
* Of course, this means that I would need to go back and 2025 modernize that code too, but that
  should be easier to do based on the work in M2 2025 modernized branch.
* I should also just go ahead and audit the changes between master (M1) and
  [**THE LAST PRE-2025 MODERNIZATION COMMIT**](https://github.com/swooby/alfred.2017/tree/last-pre-2025-modernize)
  on M2:  
  https://github.com/swooby/alfred.2017/compare/master...last-pre-2025-modernize?w=1


### 2017/04/20:

For future reference, here is Zuck's results from 2016:
* https://www.facebook.com/notes/mark-zuckerberg/building-jarvis/10154361492931634/
And a few other related articles:
* https://www.youtube.com/watch?v=AFvPeZXqU5A
* http://www.theverge.com/2016/12/20/14028744/mark-zuckerberg-jarvis-home-ai-video-watch
* https://techcrunch.com/2016/12/20/watch-mark-zuckerbergs-morgan-freeman-voiced-jarvis-ai-in-action/
* https://www.washingtonpost.com/news/business/wp/2016/12/20/mark-zuckerberg-builds-an-artificial-intelligence-assistant-to-run-his-house-and-entertain-his-toddler/
* https://www.engadget.com/2016/12/19/mark-zuckerberg-on-writing-his-own-ai/
* https://www.fastcompany.com/3066478/mark-zuckerberg-jarvis

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

(aka: "If Mark Zuckerberg can attempt to do it, then why can't I?")
(answer: probably because he has billions of dollars and hundreds of
assistants.)

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
