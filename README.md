# msscore

Write, play and visualize scores in **[MusicScene](https://github.com/shimpe/musicscene)** from
[Panola](https://github.com/shimpe/panola) strings, with one call.

`MSScore` turns one or more Panola strings into a music-notation score in MusicScene — a Godot addon that
engraves notation (via Verovio) and plays it in a 2D/3D scene over OSC. From a single call it builds MEI
(with Panola's `scoreAsMEI`), shows the notation, plays the voices, and follows along with a note-accurate
cursor that turns pages automatically for long scores.

## Requirements

- The [Panola](https://github.com/shimpe/panola) quark (pulled in as a dependency).
- A running **MusicScene** instance with Verovio working (`pip install verovio`).

## Install

Evaluate in the SuperCollider IDE:

```supercollider
Quarks.install("https://github.com/shimpe/msscore");
```

(this also installs Panola), then recompile the class library.

## Example

```supercollider
(
~score = MSScore(
    voices: [ "c5_4 e5 g5 c6", "<c4_4 e4 g4> <c4_4 e4 g4> r_4 <b3_4 d4 g4>", "c3_2 g3_2" ],
    clefs:  [\treble, \treble, \bass],
    meter: "4/4", key: \Cmajor, braces: [[2, 3]], tempo: 84, space: "2d", scale: 0.9
);
)
~score.play;   // display the notation, play the voices, follow with the cursor
~score.stop;   // stop, free synths, clear
```

See the help for the `MSScore` class for the full argument list.

## Documentation

The HelpSource is generated from the class sources with
[whelk](https://github.com/shimpe/whelk); run `gendoc.bat` (Windows) or `gendoc.sh` (Unix) after editing
the doc comments in `Classes/MSScore.sc`.

## License

GPL — see [LICENSE](LICENSE).
