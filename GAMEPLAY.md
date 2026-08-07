# RINGSHIFT — how the game works

A plain-English description of everything in the game: what you do, what every
object does, how scoring works, what each screen is for, and exactly what ends
up on the shareable card.

No code, no jargon. If something here disagrees with the game, the game is
right and this document is wrong.

---

## 1. The idea in one paragraph

You are flying down a tunnel. Rings come at you out of the dark, and each ring
is a circle cut into coloured slices with a gap or two in it. To get through a
ring you must either line up a gap with your ship, or be wearing the same
colour as the slice you meet. You do both with one thumb: **drag to turn the
tunnel, tap to change your colour**. Every ring you pass makes you faster, and
the whole game is the argument between "take the safe gap" and "take the
colour, because the colour is worth points".

---

## 2. What is on screen

**The tunnel.** A long cylinder you are flying down. It has walls, glowing
rails running to the vanishing point, and drifting debris. The speed streaks
along its walls are motion trails, so when you turn they **spiral** — their far
ends are older, and the world moved while they were being laid down. Which way
they wind tells you which way you are turning. Its colour scheme
changes every ten levels.

**Your ship.** Sits at the bottom-centre, always. It never moves left or right
— *the world turns around it*. The ship is painted in your current colour and
carries a small shape badge above it showing that colour's symbol.

**The rings.** Coming toward you from the vanishing point. Each is divided into
wedges — six, eight, ten or twelve of them depending on the sector.

**The HUD.**
- Top left: **SIGNAL** — your score this run.
- Top centre: **your velocity**, as a fraction of the speed of light. It rises
  with the level and with overdrive, and when an event horizon opens — a
  slipstream, or the one at the end of a level — you fall toward it and the
  number goes with you, up to 0.999. It never reads 1.000, because that would
  be a lie and every player knows it.

  It is never quite still: the conduit breathes, and the reading wanders by
  about a hundredth of *c* while you cruise. **Turning scrubs off 0.005** for
  as long as you are doing it — banking the conduit costs you speed, and it
  comes back when you settle. It drops much harder twice — on a
  **gate**, the level's designed breather, and on the far side of a horizon,
  where the rings have stopped and you are coasting into clear air. Both are
  readout only; the ring cadence is not touched, because the reachability
  guarantee is derived from it.
- Top right: **LV n  x/y** — the level, and how many rings you have cleared of
  the target. Tap it to pause.
- Middle, when you have a chain going: your **multiplier** and chain length.
- A thin bar along the bottom: your **resonance charge**.
- Bottom: pause, sound and effects buttons.

---

## 2a. The countdown

Every run opens stopped. Not slow — **stopped**. The conduit does not scroll,
the stars are points rather than streaks, the engine is silent, and the
velocity readout says **0.000**, because it is telling you the truth.

Over that, three numbers, each with a beep and a heartbeat under it — the
tunnel breathing faintly on every one, and nothing else on screen at all: the
side labels and everything else wait their turn. The three beeps are identical
and the one on GO is an octave up, which is the shape every start line uses,
and uses because you can hear the last one coming without being told.

Then **GO!**, and everything arrives at once — the music starts, the pace
climbs to what the level asked for over about a second and a half, and the
stars draw out into streaks as you gather speed. Nothing in the launch is
animated by hand. The tunnel, the streaks, the drone and the readout were all
reading your speed the whole time; the only thing that changes at GO is that
your speed stops being zero.

After that you get a few seconds of empty conduit before the first ring, so
the acceleration is something you watch rather than something you survive.

Pausing on the line resumes to the line. A mechanic that has to be explained
before you launch stops the count where it is, and it picks up where it left
off — you never lose a beat of it to reading.

---

## 3. Controls

The whole game is one thumb.

| You do | It does |
|---|---|
| **Drag anywhere, left or right** | Rotates the tunnel around your ship. This is how you line up a gap or bring a colour to you. |
| **Tap the LEFT half** | Changes your colour to the next one. |
| **Tap the RIGHT half** | Fires resonance (see §8). |

A drag and a tap are told apart by distance, not time — if your thumb moved
more than a few pixels it was a drag, otherwise it was a tap. There is no
press-and-hold anywhere in the game.

You can swap which half does what in Settings, and invert the drag direction.
Arrow keys and A/D also steer, if you are playing on a desktop.

---

## 4. The two verbs

Everything in the game is built out of these two, and the whole design rests on
them having *separate jobs*:

- **Colour is how you score.** Matching a slice pays; taking a gap barely does.
- **Position is how you survive.** Turning the tunnel is what lets you choose
  what you meet.

Exactly one mechanic in the game swaps those jobs over, and it is deliberately
rare — see **Inversion** (§16).

---

## 5. The rings you will meet

**Standard ring.** Coloured wedges, one or more dark gaps. Match a colour or
take a gap.

**Gap.** The dark slices. Always safe, always passable, worth almost nothing.
There is always at least one route through a ring — the game guarantees it, and
guarantees you have time to turn far enough to reach it.

**Bonus ring (white).** No colour to match, cannot hurt you, pays well. Free
money.

**Gate ring.** Mostly gaps. Just an easy ring — nothing new to learn.

**Spinning ring.** Every ring rotates slowly. From level 4 you also meet ones
that spin *much* faster, so you have to lead them like a moving target.

**Phasing ring (purple hoop).** Its colours swap around as it travels toward
you. What you saw at a distance is not what arrives.

**Hardened ring (red hoop).** Resonance will not rewrite it. You must match it
or find a gap. Firing at one wastes the charge, and the ring shudders and
throws a **RESISTED** message so you know it refused rather than glitched.

**Boss ring.** The last ring of every fifth level. It has **no gaps at all** —
the only way through is to be wearing the colour of the wedge you meet. It also
**reverses your steering** for as long as it is on screen, and it arrives a beat
later than the normal rhythm so you get a moment to read it. Worth a lot.

**The red door and the green door.** Solid, unbroken, no wedges and no symbol.
They cannot kill you and they score nothing. Passing the red one turns the
world upside down; the green one turns it back. See §16.

---

## 6. Colour and shape

Levels use **three colours** up to level 17 and **four** from level 18 on. Each
colour also owns a **shape** — circle, triangle, square, diamond — stamped on
every wedge that wears it.

The fourth colour is the single largest step in the game: it lengthens the tap
cycle every ring is solved with. It is on the schedule in §15 for that reason,
rather than falling out of a difficulty threshold.

The shapes are not decoration. Roughly one man in twelve has some red-green
colour deficiency, and several of the sector palettes are exactly the pairs
that collapse. So the shapes are **forced on for the first ten levels no matter
what the setting says**, because a player who switches them off in the first
hour has thrown away the only channel that still works before learning what any
of the shapes mean. After level 10 the toggle does what it says.

---

## 7. Scoring

| Event | Points |
|---|---|
| Take a gap | 1 |
| Match a colour | 2 |
| Match a colour **on the edge** of a wedge (a "razor") | 5 |
| Bonus ring | 8 |
| Boss ring | 50 |

Everything except the gap is multiplied by your **chain multiplier**. A gap is
always worth 1 whatever your multiplier is — that is the point of it. All of it
doubles while Overdrive is burning.

**A razor** is a match made within a whisker of the boundary between two
wedges. It is worth two and a half times a normal match and it is entirely a
skill payment — you get it for cutting it fine.

**Rings you rewrote with resonance pay 45% less.** You made that one easy, so
it pays like it.

### Chain and multiplier

Every ring you **match** adds to your **chain**. Every 5 links adds +1 to your
multiplier, up to **x8**. Dying resets it to nothing.

A gap neither grows the chain nor breaks it — it holds. That is exactly what
makes it the safe option: you can always take one without losing what you have
built, but you cannot build anything with it either. (The HALO hull is the one
exception, and it breaks your chain on a gap deliberately.)

Every tenth link of a chain fires a small celebration and a rising three-note
sting.

---

## 8. Resonance — the offensive verb

Every other game in this shape is purely defensive: the world is fixed and
hostile and you survive it. Resonance is the thing RINGSHIFT does that they do
not — **you can reach forward and rewrite the world**.

Tap the right half of the screen and you fire a pulse up the tunnel. It travels
forward, and for the next **three rings** it repaints the wedge at your contact
point into *your* colour. Those rings become guaranteed matches.

- It costs **about a third of your charge meter** per shot.
- Charge is earned by playing: a match gives a little, a razor more than twice
  as much, a bonus ring more again. Taking gaps earns nothing.
- It does not work on hardened rings, and it is unavailable during Overdrive
  because the meter is already being spent.

Different ships change its reach, width, speed and cost — see §19.

---

## 9. Overdrive

Fill the charge meter completely and it ignites by itself.

For **eight seconds**: everything scores **double**, the rings come faster, the
music switches to a wobbling bass line, and the screen edges glow amber. You
cannot fire resonance while it burns — the meter is already being spent.

It is a meter, not a one-shot: it charges, fires, drains and recharges, so a
long run has a rhythm you can keep riding.

---

## 10. Shield orbs

A gold orb occasionally drifts down the tunnel. Line it up with your ship and
fly through it and you gain a **shield**, which absorbs exactly one hit that
would otherwise have killed you.

You will see **SHIELD BURNED** when it saves you.

---

## 11. Dying

You die by meeting a wedge whose colour is not yours. The game tells you
exactly what happened in shapes, not colours — *"you hit the triangle wearing
the circle"* — so the explanation works for a colour-blind player too.

The death itself is a full sequence: the world nearly stops, the camera shakes,
the ship physically comes apart into fragments that tumble past you, and the
sound falls away into four clipped blips like a signal being lost. The game is
about a signal; losing is losing the signal.

---

## 12. Continuing after a death

Two options on the game-over card:

- **Retry** — start the level again. Free, unlimited. If you have banked a
  checkpoint (§18) it offers to restart from there instead.
- **Continue this run** — costs **credits**, keeps your score and chain. The
  first costs 150, and the price **doubles** each time. Three per run maximum.

---

## 13. Clearing a level, and stars

Clear the level's ring target and you get one to three stars:

| Stars | Requirement |
|---|---|
| ★ | Clear the level at all |
| ★★ | Match (rather than gap) **65%** of the rings |
| ★★★ | Match **90%** of the rings |

So the safe, gap-only line always clears a level and always scores exactly one
star. Stars are a measurement of how *well* you flew, not whether you survived.

**A level cleared from a checkpoint caps at two stars**, because three has to
keep meaning "in one line".

---

## 14. The send-off

Clearing a level does not just stop. The camera coasts to a halt while your
ship keeps going — accelerating away down the axis, falling off its lane, into
an **event horizon** that opens at the vanishing point.

It is a real gravitational lens, not a picture of one: the tunnel behind it
bends and winds around it, an accretion disc turns with a bright doppler-lit
edge, the shadow is absolutely black, and a thin photon ring marks the last
orbit before the fall. Your ship crosses it, there is one hard flash, and the
result card arrives.

**The same horizon opens once more, before you have played anything.** The
studio card at boot does not cross-fade into the menu — it falls in. The
horizon opens behind it, the card's black drops away to reveal it, and the mark
spirals down into the disc.

Then it does not stop. It keeps eating: the conduit winds in after the mark,
the photon ring sweeps out past the edges of the screen, and for a beat there
is **nothing on screen at all**. Not a fade to black — a swallow. The shadow
of a horizon is applied after bloom, trail, streak and grain, because nothing
escapes one, so the frame goes genuinely and completely dark by the same
shader that drew the hole. Out of that, we open on the menu.

It is not a boot-time imitation of the effect: it is the effect, the same disc,
the same shader, the same lens bending the same conduit. A cross-fade is the
one transition that says nothing about where you have been or where you are
going.

---

## 14a. The journey — why any of this is happening

You are coming home from Andromeda.

Fly the whole campaign and the ship covers about **seven astronomical units** —
Sun to just past Jupiter. Two hundred and forty-six worlds do not fit in seven
AU, and that contradiction is the story: **you are not flying the distance.**
You are flying the corridors between the horizons, and every horizon puts you
somewhere you could never have flown to. There are 1,233 of them in a
campaign — 987 slipstreams and 246 send-offs — and they carry two and a half
million light years between them.

The velocity readout is the proof, and it was telling the truth before there
was a story. It tops out at 0.999 and can never say more, because a
displacement is not a velocity. The instrument measures your motion through
the conduit; the jump is not motion through anything. Your own ship cannot
measure what is happening to it.

### The counter

A real number, on the map, on the menu tile and on every level-clear card:
**how far is left**. It starts at 2,537,000 light years and it only ever goes
one way. Every figure on the route is a real distance to a real place, in the
order you would meet them coming home:

| | | |
|---|---|---|
| Sector 1 | Andromeda, outer arm | 2,537,000 ly |
| Sector 11 | The Sagittarius Stream | 300,000 ly |
| Sector 14 | The galactic halo | 92,000 ly |
| Sector 16 | The Perseus Arm | 6,400 ly |
| Sector 20 | The Pleiades | 444 ly |
| Sector 24 | Alpha Centauri | 4.37 ly |
| Sector 25 | The Solar System | 120 AU |

The unit changes to AU in the last sector, because nobody says "0.0019 light
years" — and the switch itself tells you that you are nearly there. Level 246
begins two AU out. That last leg is the only part of the whole journey you
actually fly.

### ECHO

The log is written by the ship. ECHO is an Earth-built mind that has been out
here far longer than you have, and it has been the only voice on the channel
for a very long time — so it opens clipped and procedural, keeping the log
because keeping the log is the job, and it thaws as the number falls. One
entry a sector, given to you at the crossing and kept in **the ship's log** in
the console, which is the only file in the manual that is not finished. The
sectors ahead of you are listed and not recovered.

By the last sector it is not hiding anything, and the arrival is its
homecoming as much as yours.

---

## 14b. Coming home

The last horizon does not cut to a card.

Two hundred and forty-five times, clearing a level has meant flying into a
horizon and landing on a score screen. Level 246 goes in and then comes **out**
— black for a beat, then the sky, then our star off the port bow, and then
Earth, dead ahead and full in the frame. Terminator, weather, and the dark
side lit up with cities.

It is the real planet. Everything else in this game is generated — the
conduit, the rings, the hulls, the worlds on the chart are all noise and
mathematics — but continents are data, not a pattern, and the whole point of
the last shot is that you recognise it without being told. So the coastline
is the real coastline.

Nothing is interactive until ECHO has finished. There is no score on screen
and no card, because a number is the wrong thing to put in front of somebody
at the exact moment they arrive.

Then the autopilot takes it. Your ship lights up where it has sat for the
whole game — same lane, same height, same size — and flies away toward the
planet, and **the camera does not follow it.** It stays exactly where it has
been and watches the thing leave. Being left behind by your own ship is the
point; a camera that chased it would throw that away.

Black, and then the **logbook**: 246 worlds, your stars, your best signal, and
two and a half million light years. From there, the main menu.

Once you have arrived, the logbook has a permanent home — an icon in the top
row of the main menu, next to Settings and the manual. It is a trophy rather
than a result card, so it does not go away.

A tap once ECHO has finished brings the departure forward. It never skips
Earth and it never skips the words: on a first arrival there is nothing to
rush, and on a replay it is the difference between a homecoming and a wait.

---

## 15. The campaign

**246 levels**, and no two of them are the same level.

That claim used to rest on nothing. Every hazard rule saturated by the fourth
sector, so from level 31 onward a level's contents depended only on its
position within its sector — level 31 and level 241 carried exactly the same
mechanics, and only speed and length still moved. There were 26 distinct hazard
combinations in the whole campaign.

What a level *is* is now drawn per level (§15, "Hazards are drawn, not
accumulated"), which gives 197 distinct combinations across the 246 — on top of
the eight palettes, the ring counts, and the speed.

Levels are grouped in **sectors of ten**, each with its own colour scheme and
tunnel style:

> NEON · MAGMA · ABYSS · TOXIC · VIOLET · EMBER · GLACIER · VOID

Within each sector the difficulty climbs, dips slightly, then spikes: position
8 is a **hard** level and position 10 is an **extreme** one, so every ten levels
has a shape to it rather than being a flat ramp.

Levels get longer as you go — 12 rings at the start, 32 by level 23, and slowly
up to a ceiling of **56**, which is about 35 seconds of flying. Length is capped
on purpose: dying 50 rings into a 200-ring level converts difficulty into
tolerance for repetition.

### The hazards, in the order you meet them

Every mechanic has a level it is introduced on, and the gaps widen as they go —
five levels between the first two, thirteen between the last two, because a
player who has learned nine things needs longer with the ninth than a player
who has learned one needed with the first.

| From | Hazard | What it does |
|---|---|---|
| L4 | **Fast rings** | Some rings spin far quicker than the ones you have been reading |
| L5 | **Boss ring** | Sealed ring at the end of every 5th level, steering reversed |
| L9 | **Late colour** | Wedges arrive blank and colour up as they close |
| L15 | **Phasing colour** | Purple rings swap their colours as they travel |
| L18 | **A fourth colour** | The tap cycle gets longer, and every ring gets harder to solve |
| L22 | **Reversing spin** | Rings change direction mid-flight |
| L29 | **Checkpoints** | See §18 |
| L30 | **Dissolving colour** | Colour drains out of the wedges — read them early |
| L37 | **Inverted stretch** | See §16 |
| L46 | **Fast pairs** | Rings arrive in twos, too close to solve separately |
| L57 | **Strobing wedges** | The wedges flicker; what you saw is still what is there |
| L69 | **Hardened rings** | Resonance will not rewrite them |
| L82 | **Tunnel blackout** | The lights go out in waves; the rings stay lit |
| L96 | **Drift** | The whole tunnel slowly turns under you |
| L109 | **Reversed steering** | The conduit turns the opposite way for a whole level |

**The last new mechanic arrives on level 109.** There is no point in the
campaign where you have been shown everything it has.

### Hazards are drawn, not accumulated

Once a hazard has been introduced it joins a **pool**, and a level draws a
subset of that pool rather than carrying all of it.

- **How many** comes from where the level sits on its sector's curve: position
  1 carries about three even in the deepest sectors, position 10 carries about
  eight.
- **Which ones** is fixed for that level and never changes, so a level is
  always the same level.

This is what stops the back half being one flat wall of everything at once. The
first level of a sector is a breather however deep you are, and two levels in
the same position never carry the same set. Across the 246 levels there are 197
distinct hazard combinations.

A newly introduced hazard is guaranteed for the three levels after it arrives —
so you actually meet it — and the level that introduces it carries **at most
two other hazards**, so the lesson lands (§23).

**Fast pairs** deserve a note: the second ring of a pair arrives too soon to
solve on its own terms, and it is not supposed to be. It is built to be
reachable from wherever the first one leaves you. Take the first cleanly and
the second is already lined up.

---

## 16. Inversion — when the two verbs swap jobs

A handful of levels contain a stretch where the whole game turns over.

You pass through a **solid red door** and:

- **Your colour is no longer yours to choose.** Tapping does nothing.
- Instead, your colour **advances by one every time you match a ring**.
- **It holds if you take a gap.**
- So the only way to meet the colour you have been given is to **turn the
  tunnel** until the right wedge is in front of you.

Colour becomes the thing happening *to* you, and position becomes the thing you
score with. A **solid green door** later in the level hands it back.

Three things make it fair rather than cruel:

- The advance is **+1 and never random**, so you can always know what you will
  be wearing on the next ring.
- **Every ring in the stretch carries every colour**, so you are never told to
  match something that is not there.
- **Taking a gap holds your colour.** This is the first time in the game that
  the safe option is a *plan* — you take a gap deliberately to stall on a
  colour you can still use.

Resonance inverts with it: normally you spend charge to rewrite the world to
match you; inverted, you spend the same charge to **rewrite yourself** to match
the world, snapping your colour to the nearest coloured wedge on the ring
ahead. Same cost, opposite direction.

While it is running, a red **GLYPH LOCKED** badge stays on screen and the
screen edges breathe red.

---

## 17. The slipstream

**Every ten rings, a miniature event horizon opens** at the vanishing point.
Everything still in flight is drawn into it, you are thrown through, and a
fresh set of rings comes at you on the far side.

It is designed so it can never rob you:

- Rings caught in it are **swallowed, not missed** — they cannot kill you and
  they do not count against your clear.
- A resonance pulse in flight is **refunded**, because it was aimed at rings
  that stopped existing.
- The far side always gives you a full lead-in, so you never land on a ring you
  cannot reach.

You cannot read past a jump. Whatever is on the far side, you meet cold. That
is the point — it resets the tension every ten rings and stops a long level
being one flat stream.

*(A first-time player's very first level does not slipstream — the tutorial
gets a clean first minute. It returns immediately after.)*

---

## 18. Checkpoints

On levels of 33 rings or more, the game **banks your progress every 24 rings**.
Die after that and Retry offers to restart from the banked ring with your score,
chain and run history intact.

This exists so that what a death *costs you* stops growing as levels get
longer. A resumed clear still counts as a clear, but caps at two stars.

---

## 19. Ships

Six hulls, unlocked with total stars earned:

| Ship | Stars | What it changes |
|---|---|---|
| **DART** | 0 | Balanced. Nothing fancy, nothing in the way. |
| **DELTA** | 25 | Wide resonance — converts two wedges a ring. Razors land far more often, turning is slower. |
| **TALON** | 60 | Cheap fast pulses, short reach. Spins far faster, but the razor window is tight. |
| **WRAITH** | 110 | Starts shielded, regrows one every 20 rings. Cheap short pulses; multiplier climbs slower. |
| **MONOLITH** | 175 | Long-range resonance, five rings deep. Overdrive burns 50% longer. Turns like a freighter. |
| **HALO** | 260 | Wide, deep, expensive resonance. Opens at x2 — but gaps break your chain. |

HALO is the interesting one: it starts you at a x2 multiplier but takes away
the free safety of gaps, so it is a pure aggression build.

The costs are set against a player averaging about two stars a level, which
puts DELTA around level 15, WRAITH around 60 and HALO somewhere past 130. Three
stars a level is the maximum, so replaying an early level for its third star is
the intended way to pull any of them closer — that is what stars are *for*.

---

## 20. Credits

You earn **one credit per point of score** when you clear a level. Credits pay
for continuing a run after a death, and are the reward for daily missions.
Nothing else costs money and there is nothing to buy with real currency.

---

## 21. The daily conduit

One extra level a day, the **same for everybody**, generated from the date
itself so no server is needed. **24 rings, one attempt, no retries and no
continues.** Miss it and it is gone.

You get a star rating and a score, a **day streak** if you played yesterday
too, and a shareable card of its own (§25).

Separately there are **three daily missions** — clear some levels, thread some
rings, reach a chain of a certain length. Completing all three pays **300
credits**. They are announced once when you complete them, not every run.

---

## 22. The level map

A vertical map of the campaign drawn as a star system: each level is a body —
planet, moon or sun — with its stars underneath, joined by a dotted flight path.
Sector banners break it into named systems.

The panel at the bottom describes the level you are about to play: its ring
count, its star requirements, and every hazard it contains, named.

---

## 23. How the game teaches you

The game is mechanically heavy and gets heavier for two hundred levels, so
**every mechanic is introduced exactly once, at the moment it first appears,
and never mentioned again.**

Two kinds of introduction:

- **A briefing card, which stops the world.** Used for rules you would
  otherwise have to die to learn: the inverted stretch, hardened rings, the
  boss, the slipstream, overdrive, checkpoints, reversed steering, and fast
  pairs. Time freezes completely — nothing can reach you while you read — and a
  tap gives the world back exactly as it was. There are eight of these in the
  entire campaign.
- **A named band on the lead-in, which does not pause.** Used for hazards that
  explain themselves once you can see them: spinning rings, phasing colour,
  blackouts, drift, the shield orb, the bonus ring.

Briefings appear when the thing **arrives**, not when it bites you — a hardened
ring is explained the first time one enters the tunnel, not the first time you
waste a pulse on one.

**"Replay tutorial"** in Settings brings all of it back.

### The flight manual

Separately, the ship carries its own instruction system, reachable from the
menu, the map and Settings. It is a **console**, not a book: a directory of 24
files that you browse, and each one is read out to you by **ECHO** — the
ship's instructor — a line at a time, as if it were being spoken. Tap to skip
ahead; the index remembers which files you have opened.

Everything in it is generated from the game's own constants, so the manual
cannot drift out of step with the thing it describes.

---

## 24. Settings, and what the game will not let you do

- **Sound effects** and **Music** — independent volume sliders, 0–100. Zero
  reads as *Off*. Both act on the mix bus rather than on new voices, so a sound
  that is already ringing when you move the slider follows it down; you can
  open Settings mid-run and hear the change immediately.
- **Haptics** — vibration on and off.
- **Bloom & effects** — the full post-processing chain. The game turns this off
  by itself if it detects a device that cannot hold a frame rate, and says so.
- **Flicker reduction** — for photosensitivity. Removes the strobing.
- **Screen shake** — off if you find it uncomfortable.
- **Colour-blind glyphs** — shapes on every wedge. *Forced on for levels 1–10.*
- **Drag sensitivity** — 40% to 200%.
- **Invert steering** and **Swap tap sides**.
- **Reduced motion** is respected automatically if your device asks for it.

---

## 25. Sound

There is a music bed — a four-on-the-floor techno track whose tempo, layers and
density all climb with difficulty and with your chain, so the music tells you
how well you are doing. A sidechain duck makes the whole bed breathe under the
kick.

It starts on **GO**, and nowhere else. Through the countdown all you can hear
is the count: a beep and a real lub-dub heartbeat under it, one per number,
with a riser climbing under the last of them and the fourth beep an octave up
as the throttle opens. There is exactly one moment in the game where a conduit
goes live, and everything you hear is on one side of it or the other.

Underneath that is a continuous layer that is **driven by the ship rather than
triggered by events**: a thrust drone whose pitch and filter ride your speed,
bandpassed air noise over the hull, a whoosh panned to whichever side you just
turned, and a sub-bass swell whenever an event horizon is open. None of it is
ever triggered — it is simply always there, saying how fast you are going.

---

## 26. The shareable card

Hit **Share** on any result and the game renders a **three-second animated GIF**
— a square 468×468 card that replays your run — and hands it to your phone's
share sheet along with a line of text. Where a browser cannot share files, it
saves the GIF and copies the text instead.

### What it shows, top to bottom

**RINGSHIFT** — the wordmark.

**The subtitle** — `LEVEL 42 CLEAR`, or `LEVEL 42` if you died, or
`CONDUIT #17` for a daily.

**Your score**, big, under the word **SIGNAL**. It is not printed finished — it
*climbs* as the card plays.

**The strip.** A grid of tiles, one per ring you met, in the order you met them.
This is the heart of the card: it is a picture of *how* you cleared the level,
not just what you scored. Tiles appear one at a time as the card replays the run.

Each tile is **the colour you were actually wearing** when you passed that ring,
and carries **that colour's shape** so the picture survives being colour-blind
or being resized to a thumbnail:

| Tile | Meaning |
|---|---|
| Coloured, with a shape | A ring you matched, in the colour you wore |
| Coloured with a **gold border** | A razor — you cut it fine |
| Coloured with a **green bar** underneath | A ring you rewrote with resonance |
| Dark, with a **dash** | A gap you took |
| White | A bonus ring |
| Red | A boss ring |
| Purple | A shield of yours burned here |
| ✕ | Where you died |

*(The dash for a gap is deliberately not a shape — every geometric mark is
claimed by a colour, and more colours may be added later. A dash never can be.)*

**The stars** — one to three, stamped on at the end of the replay.

**Two footer lines.** The first is this run:

> `CHAIN 22 · x4 PEAK · 3 RAZOR`

The second is the level's **lifetime record** on your save:

> `CLEARED 7× · FIRST ON TRY 4 · 23M 14S TOTAL`

…or, if you have not beaten it yet:

> `11 TRIES · 6M 20S TOTAL · NOT YET CLEARED`

That second line is the part worth arguing about with a friend. It counts every
play, every clear, which attempt first beat it, and the total time you have
spent inside that level across your whole save — including runs you quit
halfway through.

### How the animation plays

The three seconds are shaped deliberately:

- **The first frame is the finished card.** Every platform — Discord, iMessage,
  Twitter — shows frame one as the still preview, so the still has to be the
  complete picture rather than an empty grid.
- It then **rewinds** and replays: the score counts up, the tiles appear one at
  a time in order, the stars stamp on at the end, and it holds.

The daily card is the same object with the conduit number, the day's ring count
and your streak, and it always draws the daily's **own** run.

---

## 27. What is saved

Everything lives in your browser's local storage on that device. There is no
account, no server and no network access of any kind — the whole game is a
single file.

Saved: your level, best level, stars per level, best score, best chain, credits,
chosen ship, every setting, which briefings you have seen, the daily's state and
streak, and the lifetime ledger per level. Also how far ECHO has written and
whether you have crossed the last horizon — the two things the journey needs
to remember.

A corrupted or hand-edited save cannot break the game — it is checked and
repaired on load. At worst you lose progress; you never lose the game.

If the device cannot do 3D graphics at all, the game says so in plain words,
explains what to try, and tells you your save is safe.

Because it is local storage, the origin the page is served from matters. In a
browser this looks after itself. In an Android WebView it does not — a
`file://` page can be given an opaque origin with no storage at all, and the
game will then run perfectly and quietly never save. That, along with the
engine version the page needs and the three other WebView defaults that are
wrong for it, is written up in `WEBVIEW.md`.
