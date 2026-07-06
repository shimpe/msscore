/*
[general]
title = "MSScore"
summary = "write, play and visualize a score in MusicScene from Panola string(s)"
categories = "Notation, Utils"
related = "Classes/Panola, Classes/Pbind, Classes/Ppar"
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
	description = "per-voice pattern transform applied after the base pattern is built: nil, or a Function { |pattern, voiceIndex| newPattern }. Use it to add per-note MIDI control (CC / sustain pedal / program change) while keeping the shared clock and follow cursor."
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
	wrap = "an Array whose entries are nil or a Function { |pattern, i| newPattern } applied to a voice's built pattern - use it to add per-note MIDI control (CC / sustain pedal / program change); default: all nil"
	id = "the MusicScene object id for the score"
	space = "\"2d\" or \"3d\" — match the project's musicscene/space setting"
	scale = "on-screen size of the score (default: 2.5 in \"3d\", 0.7 in \"2d\")"
	showDelay = "seconds to let the notation render before playback starts"
	paginate = "true (default) to split a long score into auto-turning pages"
	pageHeight = "Verovio page height in units (smaller = fewer systems per page = more pages)"
	host = "the MusicScene OSC host (default: \"127.0.0.1\")"
	listenPort = "the MusicScene OSC listen port (default: 7400)"
	[classmethod.new.returns]
	what = "a new MSScore"
	*/
	*new { | voices, clefs, meter = "4/4", key = \Cmajor, braces, tempo = 84, instruments,
		backends, midiOut, channels, wrap,
		id = "score", space = "2d", scale, showDelay = 1.0, paginate = true, pageHeight = 1200,
		host = "127.0.0.1", listenPort = 7400 |
		^super.new.init(voices, clefs, meter, key, braces, tempo, instruments, backends, midiOut, channels, wrap, id, space, scale, showDelay, paginate, pageHeight, host, listenPort);
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
	host = "the OSC host"
	lport = "the OSC listen port"
	*/
	init { | v, cl, m, k, br, t, instr, bk, mo, ch, wr, i, sp, sc, sd, pg, ph, host, lport |
		voices = v.collect({ |x| x.isKindOf(Panola).if({ x }, { Panola(x) }) });
		clefs = cl ? voices.collect({ \treble });
		meter = m; key = k; braces = br; tempo = t; id = i; space = sp;
		instruments = instr ? voices.collect({ \default });
		backends = bk ? voices.collect({ \internal });
		channels = ch ? voices.collect({ |x, ix| ix });   // default: each voice on its own MIDI channel
		wrap = wr ? voices.collect({ nil });
		midiOut = mo;
		scale = sc ? (sp == "3d").if({ 2.5 }, { 0.7 });   // pass `scale:` to enlarge/shrink the score
		showDelay = sd;                                    // seconds to let the notation render before playing
		paginate = pg; pageHeight = ph;                    // split long scores into pages that turn automatically
		engine = NetAddr(host, lport);
		totalBeats = voices.collect({ |p| p.totalDuration }).maxItem;
		this.pr_validate;
	}

	/*
	[method.pr_validate]
	description = "(private) check that the per-voice arrays are parallel to voices and that \\midi voices have a usable midiOut; clamp out-of-range MIDI channels with a warning"
	*/
	pr_validate {
		var n = voices.size;
		[["clefs", clefs], ["instruments", instruments], ["backends", backends],
		 ["channels", channels], ["wrap", wrap]].do({ |pair|
			(pair[1].size != n).if({
				Error("MSScore: '" ++ pair[0] ++ "' must have one entry per voice (" ++ n ++ "), got " ++ pair[1].size ++ ".").throw;
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
	mei { ^Panola.scoreAsMEI(voices, meter, key, clefs, braces) }

	/*
	[method.show]
	description = "display the notation in MusicScene, made addressable so note positions are known for the follow cursor. Non-blocking (sends the OSC setup from a Routine)."
	*/
	show {
		var m = this.mei;
		Routine({
			var snd = { |... a| engine.sendMsg(*a); 0.02.wait };
			snd.("/ms/scene/" ++ id, "new", "notation");
			snd.("/ms/scene/" ++ id, "background", "white");
			snd.("/ms/scene/" ++ id, "scale", scale);
			if (space == "3d") { snd.("/ms/scene/" ++ id, "pos", 0.0, 0.0, 0.0) } { snd.("/ms/scene/" ++ id, "pos", 0.0, 0.0) };
			snd.("/ms/scene/" ++ id ++ "/cursor", "show", 1);
			snd.("/ms/scene/" ++ id, "paginate", paginate.if({ 1 }, { 0 }), pageHeight);
			snd.("/ms/scene/" ++ id, "addressable", 1);
			snd.("/ms/scene/" ++ id, "notationData", "mei", m);
		}).play;
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
	[method.pr_allNotesOff]
	description = "(private) send an all-notes-off (CC 123) to each \\midi voice's device and channel, so stopping mid-note leaves no hanging hardware notes; each device+channel is sent once"
	*/
	pr_allNotesOff {
		var done = [];
		backends.do({ | b, i |
			var mo, ch, k;
			if (b == \midi) {
				mo = this.pr_midiOutFor(i);
				ch = channels[i];
				k = [mo, ch];
				if (mo.notNil and: { done.includes(k).not }) {
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
