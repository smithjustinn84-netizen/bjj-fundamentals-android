// Generated from the web app by tools/web-fixtures. Do not edit: change the web app and run it again.
@file:Suppress("ktlint:standard:max-line-length", "detekt.MaxLineLength") // Prose, one sentence a line.

package dev.justinsmith.bjj.core.seed

import dev.justinsmith.bjj.core.Lesson

internal val STARTER_LESSONS: List<Lesson> = listOf(
    Lesson(
        id = "first-day-on-the-mat",
        title = "First day on the mat",
        summary = "Tap early, fall safely, stand up in base, and be the kind of training partner people trust.",
        conceptIds = listOf(
            "safety",
            "breakfalls",
            "training-partner",
            "roll-to-learn",
        ),
        positionIds = emptyList(),
        transitionIds = listOf(
            "technical-stand-up",
        ),
        practice = "3 minutes: tap-and-release drill on a slow armbar, stopping before the hips lift. 4 minutes: 10 back breakfalls and 10 side breakfalls on each side. 4 minutes: 10 technical stand-ups on each side. 4 minutes: flow-roll at 40% pace, agreeing on pace and any sore joints before you start.",
    ),
    Lesson(
        id = "surviving-underneath",
        title = "Surviving underneath",
        summary = "Stay calm under side control, build frames, and escape to guard or your knees.",
        conceptIds = listOf(
            "breathing",
            "frames",
            "elbows-in",
        ),
        positionIds = listOf(
            "side-control",
        ),
        transitionIds = listOf(
            "side-control-escape",
            "side-control-escape-knees",
            "americana-defence",
        ),
        practice = "3 minutes: partner holds side control at 30% weight while you only breathe slowly and build both frames. 5 minutes: 10 escapes to guard on each side. 4 minutes: partner blocks your bottom knee, 5 escapes to your knees on each side. 3 minutes: 5 americana defences on each side, tapping early to anything caught.",
    ),
    Lesson(
        id = "escaping-the-mount",
        title = "Escaping the mount",
        summary = "Two escapes that cover for each other: bridge and roll, and the elbow-knee escape.",
        conceptIds = listOf(
            "bridge",
            "hip-escape",
            "positional-hierarchy",
        ),
        positionIds = listOf(
            "mount",
        ),
        transitionIds = listOf(
            "upa-escape",
            "elbow-knee-escape",
        ),
        practice = "2 minutes: solo bridges and shrimps. 5 minutes: 10 bridge-and-roll escapes on each side, partner posting a hand close. 5 minutes: 10 elbow-knee escapes on each side, partner basing wide. 3 minutes: partner picks either base without saying which, and you choose the escape that fits.",
    ),
    Lesson(
        id = "inside-the-closed-guard",
        title = "Inside the closed guard",
        summary = "Posture and base on top, and the first two sweeps from the bottom.",
        conceptIds = listOf(
            "posture",
            "base",
            "grips",
        ),
        positionIds = listOf(
            "closed-guard",
        ),
        transitionIds = listOf(
            "break-closed-guard",
            "scissor-sweep",
            "hip-bump-sweep",
        ),
        practice = "3 minutes: hold posture inside the guard while your partner tries to pull you down. 4 minutes: 5 guard breaks each. 4 minutes: 10 scissor sweeps each. 4 minutes: 10 hip bump sweeps each, partner sitting back on their heels.",
    ),
    Lesson(
        id = "closed-guard-sit-up-attacks",
        title = "Closed guard: sit-up and collar attacks",
        summary = "Chain the hip bump reaction into the kimura and guillotine, and threaten the cross collar choke when they stay low.",
        conceptIds = listOf(
            "action-reaction",
            "two-on-one",
        ),
        positionIds = listOf(
            "closed-guard",
        ),
        transitionIds = listOf(
            "kimura-guard",
            "guillotine-guard",
            "cross-collar-guard",
        ),
        practice = "3 minutes each, slowly and pausing for the tap: 5 kimuras, 5 guillotines, and 5 cross collar chokes per side. Last 6 minutes: run the hip-bump to kimura to guillotine chain at half speed as your partner posts or pulls back.",
    ),
    Lesson(
        id = "closed-guard-angle-attacks",
        title = "Closed guard: cutting an angle",
        summary = "Pivot off the centre line to chain the armbar, triangle, pendulum sweep, and omoplata.",
        conceptIds = listOf(
            "angles",
            "off-balancing",
        ),
        positionIds = listOf(
            "closed-guard",
        ),
        transitionIds = listOf(
            "armbar-guard",
            "triangle-choke",
            "pendulum-sweep",
            "omoplata-guard",
        ),
        practice = "3 minutes each, slowly and pausing for the tap: 5 armbars, 5 triangles, 5 pendulum sweeps, and 5 omoplatas per side. Last 3 minutes: partner stacks or pulls an arm out at half speed and you switch to the follow-up their reaction opens.",
    ),
    Lesson(
        id = "defending-inside-the-guard",
        title = "Defending inside the guard",
        summary = "Stop each closed guard submission at its first step, before it is locked.",
        conceptIds = listOf(
            "defend-early",
            "posture",
        ),
        positionIds = listOf(
            "closed-guard",
        ),
        transitionIds = listOf(
            "armbar-defence",
            "triangle-defence",
            "guillotine-defence",
            "kimura-defence",
        ),
        practice = "3 minutes each: partner sets up the armbar, triangle, guillotine and kimura to the halfway point and stops; you defend from there. Last 3 minutes: partner attacks with any of the four at half speed.",
    ),
    Lesson(
        id = "open-guard-and-passing",
        title = "Open guard and passing",
        summary = "Keep your feet and knees between you and them underneath; control the hips to pass on top.",
        conceptIds = listOf(
            "distance",
            "inside-position",
            "hip-control",
            "wedges",
        ),
        positionIds = listOf(
            "open-guard",
        ),
        transitionIds = listOf(
            "toreando-pass",
            "double-under-pass",
            "butterfly-sweep",
            "tripod-sweep",
            "guard-retention",
        ),
        practice = "3 minutes: guard retention, partner walking round your legs at half speed. 3 minutes each: 5 toreando passes, 5 double-under passes, 5 butterfly sweeps, 5 tripod sweeps per person.",
    ),
    Lesson(
        id = "half-guard-and-side-control",
        title = "Half guard and the side control pin",
        summary = "Win the underhook and head position in half guard, then pin from side control and move to mount.",
        conceptIds = listOf(
            "underhooks",
            "head-position",
            "pressure",
            "face-your-opponent",
            "crossface",
        ),
        positionIds = listOf(
            "half-guard",
            "side-control",
        ),
        transitionIds = listOf(
            "knee-slice",
            "half-guard-to-mount",
            "half-to-full-guard",
            "half-guard-underhook-sweep",
            "knee-shield-sweep",
            "side-to-mount",
            "americana",
        ),
        practice = "2 minutes: from half guard, both fight only for the underhook. 5 minutes on top: 5 knee slice passes and 5 passes straight to mount each. 5 minutes underneath: 5 recoveries to full guard, 5 underhook sweeps and 5 knee shield sweeps each. 3 minutes: hold side control for 20 seconds, slide to mount, and finish 5 slow americanas each.",
    ),
    Lesson(
        id = "attacking-from-the-pins",
        title = "Attacking from the pins",
        summary = "Settle the pin first, then attack the neck and arms or take the back when they turn to escape.",
        conceptIds = listOf(
            "position-before-submission",
        ),
        positionIds = listOf(
            "side-control",
            "mount",
        ),
        transitionIds = listOf(
            "cross-collar-choke",
            "armbar-mount",
            "mount-to-back",
            "kimura-side-control",
            "side-control-to-back",
            "americana-mount",
            "ezekiel-mount",
        ),
        practice = "3 minutes: hold side control, then mount, for 30 seconds each against half-strength escapes. 6 minutes from mount, slowly and stopping at the first resistance: 5 cross collar chokes, 5 armbars, 5 back takes as they turn, plus 3 americanas and 3 Ezekiel chokes. 6 minutes from side control: 5 kimuras and 5 gift wraps to the back.",
    ),
    Lesson(
        id = "back-control",
        title = "Back control",
        summary = "Protect your neck and escape when your back is taken, then finish from the back yourself.",
        conceptIds = listOf(
            "defend-early",
            "two-on-one",
        ),
        positionIds = listOf(
            "back-control",
        ),
        transitionIds = listOf(
            "rear-naked-choke-defence",
            "back-escape",
            "rear-naked-choke",
            "sliding-collar-choke",
        ),
        practice = "3 minutes: partner on your back with a seatbelt tries only to get a hand to your neck while you win the hand fight with two hands on one. 4 minutes: 5 choke defences and 5 back escapes each. 4 minutes each, slowly and tapping early: 5 rear naked chokes, then 5 sliding collar chokes.",
    ),
    Lesson(
        id = "turtle-basics",
        title = "Turtle",
        summary = "Seal your elbows to your knees and get out underneath; break them down or take the back on top.",
        conceptIds = listOf(
            "elbows-in",
            "base",
        ),
        positionIds = listOf(
            "turtle",
        ),
        transitionIds = listOf(
            "turtle-recover-guard",
            "turtle-sit-out",
            "turtle-to-back",
            "turtle-breakdown",
        ),
        practice = "4 minutes underneath: 5 guard recoveries and 5 sit-outs on each side. 4 minutes on top: 5 back takes and 5 breakdowns to side control on each side. 7 minutes: positional rounds from turtle, resetting when bottom recovers guard or top secures side control or hooks.",
    ),
    Lesson(
        id = "standing-and-takedowns",
        title = "Standing and takedowns",
        summary = "Win grips and off-balance first, defend leg shots with a sprawl, and take the fight to the mat cleanly.",
        conceptIds = listOf(
            "off-balancing",
            "grips",
        ),
        positionIds = listOf(
            "standing",
        ),
        transitionIds = listOf(
            "pull-guard",
            "double-leg",
            "single-leg",
            "hip-throw",
            "sprawl",
        ),
        practice = "3 minutes: grip fight standing and off-balance your partner without throwing. 4 minutes: 5 guard pulls each and 10 sprawls against slow shots. 8 minutes at walking pace: 5 double legs, 5 single legs, and 5 hip throw fit-ins on each side.",
    ),
)
