/*
[general]
title = "MSScore"
summary = "write, play and visualize a score in MusicScene from Panola string(s)"
categories = "Notation, Utils"
related = "Classes/Panola, Classes/Pbind, Classes/Pbindf, Classes/Ppar"
description = '''
MSScore turns one or more link::Classes/Panola:: strings into a music-notation score in
link::https://github.com/shimpe/musicscene##MusicScene:: — a Godot addon that engraves notation (via
Verovio) and plays it in a 2D or 3D scene, driven over OSC. From a single call it builds MEI with
link::Classes/Panola::'s teletype::scoreAsMEI::, shows the notation, plays the voices (an
link::Classes/Ppar:: of each voice's teletype::asPbind::), and follows along with a note-accurate cursor.

The voices may be Panola strings (wrapped automatically) or ready link::Classes/Panola:: instances. A long
score is split into pages that turn automatically as the cursor reaches them. The cursor needs no reply
round-trip: MusicScene is made addressable (it knows every note's on-page position and staff-system), and
MSScore simply tells it "the cursor is at beat N" on its own audio clock — so one clock drives both the
audio and the cursor and they stay in sync.

code::
(
~score = MSScore(
    voices: [ "c5_4 e5 g5 c6", "<c4_4 e4 g4> <c4_4 e4 g4> r_4 <b3_4 d4 g4>", "c3_2 g3_2" ],
    clefs:  [\treble, \treble, \bass],
    meter: "4/4", key: \Cmajor, braces: [[2,3]], tempo: 84, space: "2d", scale: 0.9
);
)
~score.play;   // display the notation, play the voices, follow with the cursor
~score.stop;   // stop, free synths, clear
::

strong::Display only:: — to show the notation without playing it or drawing a cursor, use
teletype::showPage(n):: (page teletype::n::, 1-based; default 1). Once shown, teletype::page(n)::,
teletype::nextPage:: and teletype::prevPage:: flip between pages. Distinct pages need a paginated
score (the default).

strong::Forced breaks:: - teletype::pageBreaks: [5, 9]:: starts a new page at those bars (manual pagination: you control every page boundary, auto page-fill is off), and teletype::systemBreaks: [3]:: starts a new line while keeping auto pagination. Use with teletype::paginate: true::.

strong::Lyrics:: engrave sung text under each staff: pass teletype::lyrics::, an Array parallel to teletype::voices:: (each entry a list of verse-line Strings, a bare String for one verse, or nil). Whitespace separates words, teletype::-:: separates syllables (a hyphen is drawn), teletype::_:: is a melisma, and a backslash escapes the next character. Lyrics are notation only and never affect playback; for the full rules see link::Classes/PanolaMEI::.

strong::Mid-piece meter / key changes:: - pass strong::changes::, an Array of change Events applied at
measure starts, to switch meter and/or key mid-score (each omitted field carries forward until the next
change):
code::
changes: [ ( measure: 1, meter: "4/4", key: \Cmajor ),
           ( measure: 5, meter: "3/4" ),          // meter change; key carries over
           ( measure: 9, key: \Gmajor ) ]         // key change; meter carries over
::
When strong::changes:: is set it supersedes teletype::meter::/teletype::key::. A strong::clef:: change is
instead inline in the link::Classes/Panola:: stream: tag a note teletype::@clef^bass^:: (or
teletype::\treble::/teletype::\alto::/teletype::\tenor::) to switch that staff's clef at that note,
mid-measure allowed. A key change never transposes - pitches stay as authored, accidentals are only
respelled for the new signature.

Per-note expression. A note may carry dynamics and articulation as link::Classes/Panola:: properties,
which are engraved in the score.

strong::Dynamics:: - a one-shot teletype::@dyn^mark^:: places a dynamic mark at that note (e.g.
teletype::c5_4@dyn^mf^ e5 g5::). The mark is passed straight to Verovio; the standard marks render in the
dynamics font: teletype::pppp ppp pp p mp mf f ff fff ffff:: plus the accented ones
teletype::fp sf sfz sffz rf rfz fz sfp::.

strong::Articulation:: - a static teletype::@art[name:on]:: ... teletype::@art[name:off]:: toggles an
articulation over a passage (several layer and switch off independently); a one-shot teletype::@art^name^::
marks a single note. Supported names (the friendly name, or its MEI code): teletype::staccato:: (stacc),
teletype::staccatissimo:: (stacciss), teletype::accent:: (acc), teletype::tenuto:: (ten),
teletype::marcato:: (marc), teletype::spiccato:: (spicc). Example:
teletype::c5_4@art[stacc:on] d5 e5 f5@art[stacc:off] g5@art^accent^ a5::.

strong::Slurs:: - teletype::@slur^start^:: opens a slur and teletype::@slur^end^:: closes it (both notes
are under the arc); teletype::@slur^endstart^:: closes the open slur and opens the next at the same note
(chained phrases). One slur at a time. Example:
teletype::c5_4@slur^start^ d5 e5@slur^endstart^ f5 g5@slur^end^ a5::.

strong::MIDI / hardware synths:: - by default every voice plays on a SuperCollider synth (backend
teletype::\internal::). To play a voice on an external/hardware synth instead, set strong::backends:: to
teletype::\midi:: for that voice and pass strong::midiOut:: - either a single link::Classes/MIDIOut::
shared by all teletype::\midi:: voices, or an Array of MIDIOut (one per voice). strong::channels:: gives
each voice a MIDI channel (default: the voice's index, so one multitimbral device gets a distinct channel
per staff); teletype::instruments:: applies only to teletype::\internal:: voices. For per-note MIDI
control (CC, sustain pedal, program change) pass strong::wrap::, a teletype::{ |pattern, i| newPattern }::
per voice applied to the built pattern - e.g.
teletype::{ |pat, i| Pbindf(pat, \handle, Pfunc { |ev| midiOut.control(ev[\chan], 64, (ev[\ped] ? 0).asInteger) }) }::
turns a teletype::@ped:: property into a sustain-pedal controller. strong::wrap:: changes only the
audible pattern (playback); it never affects the engraved notation, which is built from the Panola
strings directly. You create and own the MIDIOut
(teletype::MIDIClient.init; MIDIOut.newByName(...)::); MSScore never opens devices. The follow cursor works
the same over MIDI.

Requires the link::Classes/Panola:: quark, and a running MusicScene instance (with Verovio working —
teletype::pip install verovio::). Set strong::space:: to match the project's musicscene/space setting.
'''
*/
MSScore {
	/*
	[method.voices]
	description = "the score's voices, one per staff (top to bottom), as Panola instances"
	[method.voices.returns]
	what = "an Array of Panola"
	*/
	var <voices;
	/*
	[method.clefs]
	description = "the clef of each staff"
	[method.clefs.returns]
	what = "an Array of clef symbols (\\treble \\bass \\alto \\tenor)"
	*/
	var <clefs;
	/*
	[method.meter]
	description = "the time signature"
	[method.meter.returns]
	what = "a String, e.g. \"4/4\""
	*/
	var <meter;
	/*
	[method.key]
	description = "the key signature"
	[method.key.returns]
	what = "a key Symbol, e.g. \\Cmajor"
	*/
	var <key;
	/*
	[method.changes]
	description = "the mid-piece meter/key changes list (or nil for a constant score); when set it overrides meter/key"
	[method.changes.returns]
	what = "an Array of change Events ( measure:, meter:, key: ), or nil"
	*/
	var <changes;
	/*
	[method.pageBreaks]
	description = "list of 1-based measure numbers where a new PAGE starts (nil for none). Emits MEI <pb/> and switches the render to manual pagination — you control every page boundary and auto page-fill is off (Verovio limitation). Use with paginate:true; pageHeight sets the page size."
	[method.pageBreaks.returns]
	what = "an Array of measure numbers, or nil"
	*/
	var <pageBreaks;

	/*
	[method.systemBreaks]
	description = "list of 1-based measure numbers where a new SYSTEM (line) starts (nil for none). Emits MEI <sb/>; unlike pageBreaks, auto pagination is kept (pages still fill by pageHeight)."
	[method.systemBreaks.returns]
	what = "an Array of measure numbers, or nil"
	*/
	var <systemBreaks;
	/*
	[method.lyrics]
	description = "per-staff lyrics: an Array parallel to voices. Each entry is nil (no lyrics on that staff), an Array of verse-line Strings (stacked as <verse n=\"1\">, <verse n=\"2\">, ...), or a bare String (one verse). Whitespace separates words, '-' separates syllables (a hyphen is drawn), a whole-token '_' is a melisma (the next note holds the previous syllable), and '\\' escapes the next character. Notation only — lyrics never affect playback."
	[method.lyrics.returns]
	what = "an Array (parallel to voices) of verse-line lists / Strings / nil, or nil"
	*/
	var <lyrics;
	/*
	[method.notation]
	description = "the notation engine: \\verovio (default; MEI rendered by Verovio) or \\lilypond (LilyPond source rendered by the LilyPond engraver). Both paginate into auto-turning pages (use paginate:/pageHeight:/pageBreaks:); \\lilypond additionally outlines its text (lyrics/dynamics/tuplet numbers) so it shows in Godot. For \\lilypond, set the musicscene/notation/engraver/lilypond project setting to your LilyPond executable."
	[method.notation.returns]
	what = "a Symbol (\\verovio or \\lilypond)"
	*/
	var <notation;
	/*
	[method.braces]
	description = "1-based [firstStaff, lastStaff] ranges braced together (e.g. a piano grand staff)"
	[method.braces.returns]
	what = "an Array of [Integer, Integer] pairs, or nil"
	*/
	var <braces;
	/*
	[method.tempo]
	description = "the tempo in beats (quarter notes) per minute"
	[method.tempo.returns]
	what = "a Number"
	*/
	var <tempo;
	/*
	[method.id]
	description = "the MusicScene object id under which the score is created"
	[method.id.returns]
	what = "a String"
	*/
	var <id;
	/*
	[method.space]
	description = "the scene dimensionality, matching the project's musicscene/space setting"
	[method.space.returns]
	what = "\"2d\" or \"3d\""
	*/
	var <space;
	/*
	[method.instruments]
	description = "the SynthDef name to play each staff with"
	[method.instruments.returns]
	what = "an Array of Symbols"
	*/
	var <instruments;
	/*
	[method.backends]
	description = "per-voice playback backend: \\internal (SuperCollider synth) or \\midi (external/hardware synth)"
	[method.backends.returns]
	what = "an Array of Symbols (\\internal / \\midi)"
	*/
	var <backends;
	/*
	[method.midiOut]
	description = "the MIDIOut(s) used by \\midi voices: a single MIDIOut shared by all of them, or an Array of MIDIOut (one per voice)"
	[method.midiOut.returns]
	what = "a MIDIOut, an Array of MIDIOut, or nil"
	*/
	var <midiOut;
	/*
	[method.channels]
	description = "per-voice MIDI channel (0..15), used only by \\midi voices"
	[method.channels.returns]
	what = "an Array of Integers"
	*/
	var <channels;
	/*
	[method.wrap]
	description = "per-voice pattern transform applied after the base pattern is built: nil, or a Function { |pattern, voiceIndex| newPattern }. Use it to add per-note MIDI control (CC / sustain pedal / program change) while keeping the shared clock and follow cursor. Affects PLAYBACK only - it never changes the engraved notation (the score is built from the Panola strings directly)."
	[method.wrap.returns]
	what = "an Array whose entries are nil or a Function"
	*/
	var <wrap;
	/*
	[method.scale]
	description = "the on-screen size of the score (defaults to 2.5 in \"3d\", 0.7 in \"2d\")"
	[method.scale.returns]
	what = "a Number"
	*/
	var <scale;
	/*
	[method.totalBeats]
	description = "the length of the longest voice, in beats"
	[method.totalBeats.returns]
	what = "a Number"
	*/
	var <totalBeats;
	/*
	[method.showDelay]
	description = "seconds to let the notation render before playback starts"
	[method.showDelay.returns]
	what = "a Number"
	*/
	var <showDelay;
	/*
	[method.paginate]
	description = "whether a long score is split into auto-turning pages"
	[method.paginate.returns]
	what = "a Boolean"
	*/
	var <paginate;
	/*
	[method.pageHeight]
	description = "Verovio page height in units when paginating (smaller = fewer systems per page = more pages)"
	[method.pageHeight.returns]
	what = "a Number"
	*/
	var <pageHeight;
	/*
	[method.showCursor]
	description = "whether the playback cursor line is drawn; when false the score still auto-turns pages, just without a visible cursor"
	[method.showCursor.returns]
	what = "a Boolean"
	*/
	var <showCursor;
	// --- runtime state (set while playing) ---
	/*
	[method.engine]
	description = "the OSC connection to MusicScene"
	[method.engine.returns]
	what = "a NetAddr"
	*/
	var <engine;
	/*
	[method.clock]
	description = "the TempoClock driving both the audio and the cursor (nil when stopped)"
	[method.clock.returns]
	what = "a TempoClock or nil"
	*/
	var <clock;
	/*
	[method.player]
	description = "the running Ppar player of the voices (nil until playing)"
	[method.player.returns]
	what = "an EventStreamPlayer or nil"
	*/
	var <player;
	/*
	[method.cursorRoutine]
	description = "the routine that drives the follow cursor over OSC (nil until playing)"
	[method.cursorRoutine.returns]
	what = "a Routine or nil"
	*/
	var <cursorRoutine;

	/*
	[classmethod.new]
	description = "create a score from Panola voice(s) and score preferences"
	[classmethod.new.args]
	voices = "an Array of Panola strings (wrapped automatically) or Panola instances, one per staff (top first)"
	clefs = "an Array of clef symbols, one per staff (default: all \\treble)"
	meter = "time signature String, e.g. \"4/4\""
	key = "key Symbol, e.g. \\Cmajor, \\Dminor, \\CsharpMinor"
	braces = "an Array of [firstStaff, lastStaff] 1-based ranges to brace together (e.g. a piano grand staff)"
	tempo = "tempo in beats (quarter notes) per minute"
	instruments = "an Array of SynthDef names, one per staff, for \\internal voices (default: all \\default)"
	backends = "an Array of \\internal (SuperCollider synth) or \\midi (external/hardware synth), one per voice (default: all \\internal)"
	midiOut = "a MIDIOut shared by all \\midi voices, or an Array of MIDIOut (one per voice); required if any voice is \\midi"
	channels = "an Array of MIDI channels (0..15), one per voice, used only by \\midi voices (default: each voice's index)"
	wrap = "an Array whose entries are nil or a Function { |pattern, i| newPattern } applied to a voice's built pattern - use it to add per-note MIDI control (CC / sustain pedal / program change); default: all nil. Affects PLAYBACK only, never the engraved notation."
	id = "the MusicScene object id for the score"
	space = "\"2d\" or \"3d\" — match the project's musicscene/space setting"
	scale = "on-screen size of the score (default: 2.5 in \"3d\", 0.7 in \"2d\")"
	showDelay = "seconds to let the notation render before playback starts"
	paginate = "true (default) to split a long score into auto-turning pages"
	pageHeight = "Verovio page height in units (smaller = fewer systems per page = more pages)"
	showCursor = "true (default) to draw the playback cursor; false shows the score and still auto-turns pages, but without a visible cursor"
	host = "the MusicScene OSC host (default: \"127.0.0.1\")"
	listenPort = "the MusicScene OSC listen port (default: 7400)"
	changes = "an Array of mid-piece change Events applied at measure starts, e.g. [ ( measure: 1, meter: \"4/4\", key: \\Cmajor ), ( measure: 5, meter: \"3/4\" ) ]; each field carries forward until changed. When set it overrides meter/key (which then act only as fallbacks). Default: nil (a constant meter/key score). Mid-measure clef changes are inline in the Panola stream via @clef^bass^, not here."
	pageBreaks = "an Array of 1-based measure numbers where a new PAGE starts (nil for none); manual pagination, auto page-fill off. Use with paginate:true."
	systemBreaks = "an Array of 1-based measure numbers where a new SYSTEM (line) starts (nil for none); auto pagination is kept."
	lyrics = "per-staff lyrics: an Array parallel to voices, each entry nil, an Array of verse-line Strings (several verses), or a bare String (one verse). A space separates words, a hyphen separates syllables, an underscore is a melisma, and a backslash escapes the next character. Notation only."
	notation = "the notation engine: \\verovio (default, MEI + Verovio) or \\lilypond (LilyPond). Both paginate into auto-turning pages. For \\lilypond set the musicscene/notation/engraver/lilypond project setting."
	[classmethod.new.returns]
	what = "a new MSScore"
	*/
	*new { | voices, clefs, meter = "4/4", key = \Cmajor, braces, tempo = 84, instruments,
		backends, midiOut, channels, wrap,
		id = "score", space = "2d", scale, showDelay = 1.0, paginate = true, pageHeight = 1200,
		showCursor = true, host = "127.0.0.1", listenPort = 7400, changes, pageBreaks, systemBreaks, lyrics, notation = \verovio |
		^super.new.init(voices, clefs, meter, key, braces, tempo, instruments, backends, midiOut, channels, wrap, id, space, scale, showDelay, paginate, pageHeight, showCursor, host, listenPort, changes, pageBreaks, systemBreaks, lyrics, notation);
	}

	/*
	[method.init]
	description = "initialize a new MSScore (called by *new); wraps plain strings into Panola instances and applies the defaults"
	[method.init.args]
	v = "the voices (Panola strings or instances)"
	cl = "the clefs (or nil)"
	m = "the meter String"
	k = "the key Symbol"
	br = "the brace ranges (or nil)"
	t = "the tempo"
	instr = "the instrument SynthDef names (or nil)"
	bk = "the backends (\\internal / \\midi) per voice (or nil)"
	mo = "the midiOut: a MIDIOut, an Array of MIDIOut, or nil"
	ch = "the MIDI channels per voice (or nil)"
	wr = "the per-voice wrap Functions (or nil)"
	i = "the MusicScene object id"
	sp = "the space (\"2d\"/\"3d\")"
	sc = "the scale (or nil for the space-dependent default)"
	sd = "the show delay in seconds"
	pg = "paginate flag"
	ph = "page height"
	scr = "showCursor flag"
	host = "the OSC host"
	lport = "the OSC listen port"
	chg = "the mid-piece changes list (or nil for a constant meter/key score)"
	pgbr = "the page-break measure numbers (or nil)"
	sysbr = "the system-break measure numbers (or nil)"
	lyr = "the per-staff lyrics (or nil)"
	ntn = "the notation engine Symbol (or nil -> \\verovio)"
	*/
	init { | v, cl, m, k, br, t, instr, bk, mo, ch, wr, i, sp, sc, sd, pg, ph, scr, host, lport, chg, pgbr, sysbr, lyr, ntn |
		voices = v.collect({ |x| x.isKindOf(Panola).if({ x }, { Panola(x) }) });
		clefs = cl ? voices.collect({ \treble });
		meter = m; key = k; braces = br; tempo = t; id = i; space = sp;
		changes = chg;                                     // nil -> constant meter/key; else a mid-piece changes list
		pageBreaks = pgbr;                                 // nil -> no forced page breaks (auto-pagination)
		systemBreaks = sysbr;                              // nil -> no forced system/line breaks
		lyrics = lyr;                                      // nil -> no lyrics; else per-staff verse lines
		notation = ntn ? \verovio;                          // \verovio (MEI) or \lilypond — both paginate into auto-turning pages
		instruments = instr ? voices.collect({ \default });
		backends = bk ? voices.collect({ \internal });
		channels = ch ? voices.collect({ |x, ix| ix });   // default: each voice on its own MIDI channel
		wrap = wr ? voices.collect({ nil });
		midiOut = mo;
		scale = sc ? (sp == "3d").if({ 2.5 }, { 0.7 });   // pass `scale:` to enlarge/shrink the score
		showDelay = sd;                                    // seconds to let the notation render before playing
		paginate = pg; pageHeight = ph;                    // split long scores into pages that turn automatically
		showCursor = scr;                                  // draw the playback cursor line (pages still auto-turn if false)
		engine = NetAddr(host, lport);
		totalBeats = voices.collect({ |p| p.totalDuration }).maxItem;
		this.pr_validate;
	}

	/*
	[method.pr_validate]
	description = "(private) check that the per-voice arrays are parallel to voices, that each backend is \\internal or \\midi, and that \\midi voices have a usable midiOut; clamp out-of-range MIDI channels with a warning"
	*/
	pr_validate {
		var n = voices.size;
		[["clefs", clefs], ["instruments", instruments], ["backends", backends],
		 ["channels", channels], ["wrap", wrap]].do({ |pair|
			(pair[1].size != n).if({
				Error("MSScore: '" ++ pair[0] ++ "' must have one entry per voice (" ++ n ++ "), got " ++ pair[1].size ++ ".").throw;
			});
		});
		backends.do({ |b, ix|
			(#[\internal, \midi].includes(b).not).if({
				Error("MSScore: backends[" ++ ix ++ "] must be \\internal or \\midi, got " ++ b ++ ".").throw;
			});
		});
		if (backends.includes(\midi)) {
			midiOut.isNil.if({
				Error("MSScore: a \\midi voice needs a midiOut (a MIDIOut, or an Array of MIDIOut).").throw;
			});
			midiOut.isArray.if({
				(midiOut.size != n).if({
					Error("MSScore: midiOut Array must have one entry per voice (" ++ n ++ "), got " ++ midiOut.size ++ ".").throw;
				});
				backends.do({ |b, ix|
					(b == \midi and: { midiOut[ix].isNil }).if({
						Error("MSScore: midiOut[" ++ ix ++ "] is nil but voice " ++ ix ++ " is \\midi.").throw;
					});
				});
			});
			channels = channels.collect({ |c, ix|
				(backends[ix] == \midi and: { (c < 0) or: { c > 15 } }).if({
					warn("MSScore: MIDI channel " ++ c ++ " for voice " ++ ix ++ " out of 0..15; clamping.");
					c.clip(0, 15);
				}, { c });
			});
		};
	}

	/*
	[method.mei]
	description = "the MEI notation document for this score, built with link::Classes/Panola#*scoreAsMEI::. Usable on its own (e.g. to write to a .mei file for any MEI renderer)."
	[method.mei.returns]
	what = "an MEI document (a String)"
	*/
	mei { ^Panola.scoreAsMEI(voices, changes ? [( measure: 1, meter: meter, key: key )], clefs, braces, pageBreaks, systemBreaks, lyrics) }

	/*
	[method.ly]
	description = "the standalone LilyPond notation document for this score, built with link::Classes/Panola#*scoreAsLilypond::. Renders on its own (teletype::lilypond file.ly::) and is what MSScore sends when teletype::notation:: is \\lilypond."
	[method.ly.returns]
	what = "a LilyPond document (a String)"
	*/
	ly { ^Panola.scoreAsLilypond(voices, changes ? [( measure: 1, meter: meter, key: key )], clefs, braces, pageBreaks, systemBreaks, lyrics) }

	/*
	[method.pr_paginateInt]
	description = "(private) the paginate flag as 1/0 for OSC (used for both \\verovio and \\lilypond)."
	[method.pr_paginateInt.returns]
	what = "1 if paginating, else 0"
	*/
	pr_paginateInt { ^paginate.if({ 1 }, { 0 }) }

	/*
	[method.pr_emitSetup]
	description = "(private) emit the notation-display OSC setup (create node, background, scale, pos, cursor, paginate, addressable, notationData). Runs INSIDE a Routine (uses waits). cursorOn draws the cursor line or not."
	[method.pr_emitSetup.args]
	cursorOn = "true to draw the cursor line, false to hide it"
	*/
	pr_emitSetup { | cursorOn |
		var isLy = (notation == \lilypond) or: { notation == \ly };
		var fmt = isLy.if({ "lilypond" }, { "mei" }), data = isLy.if({ this.ly }, { this.mei });
		var snd = { |... a| engine.sendMsg(*a); 0.02.wait };
		snd.("/ms/scene/" ++ id, "new", "notation");
		snd.("/ms/scene/" ++ id, "background", "white");
		snd.("/ms/scene/" ++ id, "scale", scale);
		if (space == "3d") { snd.("/ms/scene/" ++ id, "pos", 0.0, 0.0, 0.0) } { snd.("/ms/scene/" ++ id, "pos", 0.0, 0.0) };
		snd.("/ms/scene/" ++ id ++ "/cursor", "show", cursorOn.if({ 1 }, { 0 }));
		snd.("/ms/scene/" ++ id, "paginate", this.pr_paginateInt, pageHeight);
		snd.("/ms/scene/" ++ id, "addressable", 1);
		snd.("/ms/scene/" ++ id, "notationData", fmt, data);
	}

	/*
	[method.show]
	description = "display the notation in MusicScene, made addressable so note positions are known for the follow cursor. Non-blocking (sends the OSC setup from a Routine)."
	*/
	show {
		Routine({ this.pr_emitSetup(showCursor); }).play;
	}

	/*
	[method.showPage]
	description = "display the notation and show a given page, with NO cursor and NO playback (display only). Non-blocking. Distinct pages need a paginated score (the default). See link::Classes/MSScore#-page::, link::Classes/MSScore#-nextPage::, link::Classes/MSScore#-prevPage::."
	[method.showPage.args]
	pageNumber = "the 1-based page to show (default 1)"
	*/
	showPage { | pageNumber = 1 |
		// send the page request right after the setup (NOT after showDelay): MusicScene remembers the
		// requested page and applies it once the async render lands, so no timer is needed — and a later
		// nextPage/prevPage/page() is not overridden by a late-firing deferred page.
		Routine({ this.pr_emitSetup(false); this.page(pageNumber); }).play;
	}

	/*
	[method.play]
	description = "show the score, then (after showDelay) play the voices and follow along with a note-accurate cursor. Non-blocking."
	*/
	play {
		this.show;
		Routine({ showDelay.wait; this.pr_startPlayback; }).play;
	}

	/*
	[method.stop]
	description = "stop playback and the cursor, free all synths on the default server, and clear the scene"
	*/
	stop {
		clock.notNil.if({ clock.stop; clock = nil });
		player.notNil.if({ player.stop });
		cursorRoutine.notNil.if({ cursorRoutine.stop });
		this.pr_allNotesOff;
		Server.default.freeAll;
		engine.sendMsg("/ms/scene", "clear");
	}

	/*
	[method.page]
	description = "on an already-shown score, jump to a 1-based page (MusicScene clamps out-of-range). Distinct pages need a paginated score (the default); otherwise MusicScene re-renders that page."
	[method.page.args]
	pageNumber = "the 1-based page to show"
	*/
	page { | pageNumber = 1 | engine.sendMsg("/ms/scene/" ++ id, "page", pageNumber); }

	/*
	[method.nextPage]
	description = "flip the shown score forward one page"
	*/
	nextPage { engine.sendMsg("/ms/scene/" ++ id, "nextpage"); }

	/*
	[method.prevPage]
	description = "flip the shown score back one page"
	*/
	prevPage { engine.sendMsg("/ms/scene/" ++ id, "prevpage"); }

	/*
	[method.pr_allNotesOff]
	description = "(private) send a MIDI All Notes Off (CC 123) to each \\midi voice's device and channel when stopping, so notes still sounding are released; each device+channel is sent once. (CC 123 does not release notes held by a sustain pedal.)"
	*/
	pr_allNotesOff {
		var done = [];
		backends.do({ | b, i |
			var mo, ch, k;
			if (b == \midi) {
				mo = this.pr_midiOutFor(i);
				ch = channels[i];
				k = [mo, ch];
				if (mo.notNil and: { done.any({ | x | x == k }).not }) {
					mo.control(ch, 123, 0);   // CC 123 = All Notes Off
					done = done.add(k);
				};
			};
		});
	}

	/*
	[method.pr_midiOutFor]
	description = "(private) the MIDIOut for voice i: the shared midiOut, or midiOut[i] when midiOut is a per-voice Array"
	[method.pr_midiOutFor.args]
	i = "the voice index"
	[method.pr_midiOutFor.returns]
	what = "a MIDIOut (or nil)"
	*/
	pr_midiOutFor { | i | ^midiOut.isArray.if({ midiOut[i] }, { midiOut }); }

	/*
	[method.pr_voicePatterns]
	description = "(private) the pattern for each voice: asPbind for an \\internal voice, asMidiPbind for a \\midi voice, then passed through this voice's wrap function if one is set"
	[method.pr_voicePatterns.returns]
	what = "an Array of patterns (one per voice), ready for a Ppar"
	*/
	pr_voicePatterns {
		^voices.collect({ | p, i |
			var pat = (backends[i] == \midi).if(
				{ p.asMidiPbind(this.pr_midiOutFor(i), channels[i], include_tempo: false) },
				{ p.asPbind(instruments[i], include_tempo: false) }
			);
			wrap[i].notNil.if({ wrap[i].value(pat, i) }, { pat });
		});
	}

	/*
	[method.pr_startPlayback]
	description = "(private) start the Ppar playback and the follow-cursor routine on one shared TempoClock, so audio and cursor stay in sync. The cursor sends its position (in whole notes = beats/4) to MusicScene, which maps it to the current note's on-page position and staff-system."
	*/
	pr_startPlayback {
		var startBeat;
		clock = TempoClock(tempo / 60);
		player = Ppar(this.pr_voicePatterns).play(clock, quant: 0);
		startBeat = clock.beats;
		cursorRoutine = Routine({
			while { (clock.beats - startBeat) <= (totalBeats + 0.5) } {
				engine.sendMsg("/ms/scene/" ++ id ++ "/cursor", "at", (clock.beats - startBeat) / 4);
				0.0625.wait;   // ~16 cursor updates per beat, for smooth motion
			};
		}).play(clock, quant: 0);
	}
}

/*
[examples]
what = '''
// =====================================================================================
// MSScore tutorial. MSScore turns one or more Panola strings into a notation score that
// is shown, played and cursor-followed inside MusicScene (a Godot app).
//
// To run the PLAYING examples you need:
//   * a booted audio server                 -> s.boot  (the examples wrap in s.waitForBoot)
//   * a running MusicScene instance with Verovio  (pip install verovio)
//   * the MSScore quark installed:  Quarks.install("https://github.com/shimpe/msscore")
// The examples play on \default; pass your own SynthDef name(s) via `instruments:`.
// Set `space:` to match your project's musicscene/space setting ("2d" or "3d").
// Every example ends with a matching `~score.stop;` you can evaluate to stop + clear.
// The very LAST example only builds the MEI and needs no MusicScene / no server.
// =====================================================================================


// -------------------------------------------------------------------------------------
// 1. The simplest score: one voice. `play` shows the notation, plays the voice, and
//    follows along with a note-accurate cursor. Durations are Panola: c5_4 = a quarter
//    note on c5, and a duration carries over until it changes (e5 g5 are quarters too).
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~score = MSScore(voices: ["c5_4 e5 g5 c6"], instruments: [\default]);
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 2. Several voices, one staff each. `clefs` sets each staff's clef; `braces` joins
//    staves with a brace (a piano grand staff here). Chords use <angle brackets>; a
//    rest is `r`.
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~score = MSScore(
        voices: [ "c5_4 e5 g5 c6", "<c4_2 e4 g4> <b3_2 d4 g4>", "c3_1" ],
        clefs:  [\treble, \treble, \bass],
        braces: [[1, 2]],                       // brace staves 1 and 2
        tempo:  96,
        scale:  1.0,
        instruments: [\default, \default, \default]
    );
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 3. Meter and key. Barlines are derived from `meter`; accidentals are spelled relative
//    to `key` (an authored f prints as f-natural in C major here).
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~score = MSScore(
        voices: [ "c5_4 d5 e5 f5 g5_2 a5 b5 c6_1" ],
        meter:  "4/4", key: \Cmajor, tempo: 100, scale: 1.0,
        instruments: [\default]
    );
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 4. Rhythm: dotted notes (a `.` after the duration) and tuplets (`*num/den`). A triplet
//    of eighths is `_8*2/3`; the ratio carries over like the duration does.
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~score = MSScore(
        voices: [ "c5_4. e5_8 g5_8*2/3 a5 b5 c6_2" ],
        meter: "4/4", key: \Cmajor, tempo: 92, scale: 1.0,
        instruments: [\default]
    );
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 5. Dynamics. A one-shot @dyn^mark^ places a dynamic at that note. Standard marks
//    render in the dynamics font (pppp..ffff, plus fp sf sfz sffz rf rfz fz sfp).
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~score = MSScore(
        voices: [ "c5_4@dyn^p^ e5 g5 c6@dyn^f^ g5_2 e5_2" ],
        tempo: 96, scale: 1.0, instruments: [\default]
    );
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 6. Articulation. @art[name:on] .. @art[name:off] toggles an articulation over a
//    passage; a one-shot @art^name^ marks a single note; combine several on one note
//    with `+`. Names (friendly or MEI code): staccato accent tenuto marcato spiccato
//    staccatissimo.
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~score = MSScore(
        voices: [ "c5_8@art[stacc:on] d5 e5 f5 g5@art[stacc:off] a5_4@art^accent^ b5_4@art^staccato+accent^ c6_2" ],
        tempo: 108, scale: 1.0, instruments: [\default]
    );
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 7. Slurs and hairpins. @slur^start^ .. @slur^end^ draws a slur (@slur^endstart^ chains
//    phrases). @hairpin^cresc^ (or ^dim^) .. @hairpin^end^ draws a crescendo/decrescendo;
//    @hairpin^endcresc^ / ^enddim^ chain a messa di voce (< >).
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~score = MSScore(
        voices: [ "c5_4@slur^start^@hairpin^cresc^ e5 g5 c6@slur^end^@hairpin^enddim^ g5 e5 c5@hairpin^end^" ],
        tempo: 88, scale: 1.0, instruments: [\default]
    );
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 8. Mid-piece meter / key changes. `changes` is a list of Events applied at the start
//    of a (1-based) measure; each omitted field carries forward. When set it supersedes
//    the plain meter/key. A clef change is instead INLINE in the Panola stream:
//    @clef^bass^ (also ^treble^/^alto^/^tenor^) switches that staff's clef at that note.
//    A key change never transposes - accidentals are only respelled for the new key.
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~score = MSScore(
        voices: [ "c5_4 e5 g5 e5 d5_4 f#5 a5 f#5 g5_4 b5 d6 b5 c6_2 g5_4@clef^bass^ c4" ],
        changes: [ ( measure: 1, meter: "4/4", key: \Cmajor ),
                   ( measure: 2, key: \Gmajor ),        // key change; meter carries over
                   ( measure: 4, meter: "3/4" ) ],      // meter change; key carries over
        tempo: 96, scale: 1.0, instruments: [\default]
    );
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 9. Forced page & system breaks (manual layout). `pageBreaks` starts a new PAGE at the
//    listed 1-based bars (manual pagination - you control every page boundary, auto
//    page-fill is off); `systemBreaks` starts a new LINE while keeping auto pagination.
//    Use with paginate:true; pageHeight sets the page size.
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~score = MSScore(
        voices: [ "c5_4 e5 g5 e5 d5_4 f5 a5 f5 e5_4 g5 b5 g5 f5_4 a5 c6 a5 g5_1" ],
        paginate: true, pageHeight: 900,
        systemBreaks: [3],                 // new line at bar 3
        pageBreaks:   [5],                 // new page at bar 5
        tempo: 92, scale: 1.0, instruments: [\default]
    );
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 10. Display only - show a given page with NO cursor and NO playback. `showPage(n)`
//     engraves and shows page n (1-based); `page(n)`, `nextPage`, `prevPage` flip between
//     pages. Distinct pages need a paginated score (a small pageHeight => more pages).
// -------------------------------------------------------------------------------------
(
~score = MSScore(
    voices: [ (1..32).collect({ |i| ["c5","e5","g5","a5","f5","d5"].wrapAt(i) ++ "_8" }).join(" ") ],
    paginate: true, pageHeight: 300,       // small page => several pages
    scale: 1.0, instruments: [\default]
);
~score.showPage(1);                        // display page 1 (no sound, no cursor)
)
~score.nextPage;                           // -> page 2
~score.prevPage;                           // -> page 1
~score.page(2);                            // jump to page 2
~score.stop;                               // clear the scene


// -------------------------------------------------------------------------------------
// 11. Play a voice on an EXTERNAL / hardware synth over MIDI. Set `backends` to \midi
//     for that voice and pass a `midiOut` (a MIDIOut you created); `channels` gives each
//     voice a MIDI channel. The follow cursor works the same over MIDI.
// -------------------------------------------------------------------------------------
(
MIDIClient.init;
~mout = MIDIOut.newByName("your device name", "your port name");   // <- your device
s.waitForBoot({
    ~score = MSScore(
        voices:   [ "c5_4 e5 g5 c6", "c3_2 g3" ],
        clefs:    [\treble, \bass],
        backends: [\midi, \internal],                 // voice 1 -> MIDI, voice 2 -> SC synth
        midiOut:  ~mout,
        channels: [1, nil],                           // voice 1 on MIDI channel 1
        instruments: [\default, \default]             // used only by the \internal voice
    );
    ~score.play;
});
)
~score.stop;


// -------------------------------------------------------------------------------------
// 12. Postprocess a voice's pattern with `wrap` - make the PRINTED marks AUDIBLE.
//     MSScore builds each voice's pattern with Panola's asPbind (asMidiPbind for a \midi
//     voice) and then hands it to your wrap Function, { |pattern, voiceIndex| newPattern }.
//     One entry per voice; nil leaves a voice alone.
//
//     Every Panola @property arrives as a Pbind key of the same name (@vol -> \amp,
//     @pdur -> \legato). The notation marks come through as Symbols - \dyn, \art, \slur,
//     \hairpin - so this is where a printed `mf` becomes a real amplitude and a printed
//     staccato dot becomes a short note. Pbindf APPENDS keys, so your Pfunc already sees
//     everything Panola computed; naming a key Panola set replaces its value.
// -------------------------------------------------------------------------------------
(
s.waitForBoot({
    ~amp = ( pp: 0.15, p: 0.25, mp: 0.35, mf: 0.5, f: 0.7, ff: 0.9 );
    ~score = MSScore(
        voices: [ "c5_4@dyn[mf]@art^staccato^ e5@art^staccato^ g5_2 c6_4@dyn[ff]@art^staccato^ g5_2" ],
        wrap: [
            { |pat, i|
                Pbindf(pat,
                    \amp,    Pfunc { |ev| ~amp[ev[\dyn]] ? 0.4 },
                    \legato, Pfunc { |ev| if (ev[\art] == \staccato) { 0.3 } { 0.9 } })
            }
        ]
    );
    ~score.play;    // 2 dynamics and 3 staccato dots on the page - and in the sound
});
)
~score.stop;

// Notes on `wrap`:
//
//  * Dynamics are printed ON CHANGE, so a static @dyn[mf] engraves exactly one `mf`, just
//    like the one-shot @dyn^mf^. They differ in the PATTERN: static carries `mf` to every
//    later note, while one-shot leaves the rest at \none. Author @dyn static when a wrap
//    function should hear the dynamic. Articulations are per-note, so static @art[staccato]
//    dots EVERY note - use the one-shot @art^staccato^ for a single dot, as above.
//  * On notes where a mark is not set you get \none, never nil or ''. A property never
//    authored anywhere in that voice has no key at all (ev[\dyn] is nil).
//  * The toggle form yields the raw string: @art[staccato:on] gives 'staccato:on' (and
//    'staccato:off' afterwards), NOT \staccato. Compare accordingly.
//  * Do NOT rewrite \dur. The follow cursor is driven by the score's own beat count on the
//    shared TempoClock, not by the pattern, so changing note durations desynchronises the
//    cursor from the audio. There is no \tempo key either - the `tempo:` argument owns the
//    clock.
//  * `wrap` also applies to \midi voices (it runs after asMidiPbind); that is how you add
//    per-note CC, sustain pedal or program change.
//  * One entry per voice, or MSScore raises "'wrap' must have one entry per voice".
//  * `wrap` changes PLAYBACK only. The engraved notation is built from the Panola strings
//    directly (scoreAsMEI), so a wrap function never changes what is PRINTED - only what is heard.
//
// The other idiom, for SIDE EFFECTS or state (a slur state machine, a MIDI controller):
// append a dummy key whose Pfunc mutates the event in place and returns 0.
//
//    Pbindf(pat, \handle, Pfunc { |ev|
//        if (ev[\art] == \stacc) { ev[\legato] = 0.5 } { ev[\legato] = 0.9 };
//        midiOut.control(0, 64, (ev[\ped] ? 0).asInteger);   // sustain pedal from @ped
//        0 })
//
// See examples/supercollider/example_msscore_midi.scd in the MusicScene repository for a
// worked version that reads \art and \slur to phrase a MIDI voice.


// -------------------------------------------------------------------------------------
// 13. No MusicScene needed: just build the MEI document (a String). Useful to inspect
//     the notation, or write it to a .mei file for any MEI renderer (Verovio, etc.).
// -------------------------------------------------------------------------------------
(
~score = MSScore(voices: [ "c5_4 e5 g5 c6" ], instruments: [\default]);
~mei = ~score.mei;                          // the MEI as a String
~mei.postln;
// File.use(Platform.defaultTempDir +/+ "score.mei", "w", { |f| f.write(~mei) });
)
'''
*/
