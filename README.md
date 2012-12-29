# ClojureSubs

One day, ClojureSubs may become a library for subtitle file handling in Clojure not unlike [PySubs](https://github.com/tigr42/pysubs/).

**Warning**: ClojureSubs should be considered *pre-alpha quality software* at this time. It may set your hair on fire at any time! Be warned.

## Features

- read and write SubRip (``*.srt``) files
- shift by given amount of time
- (not implemented yet) read and write SubStation Alpha (``*.ass``, ``*.ssa``) files
- (not implemented yet) other things you can do in [PySubs](https://github.com/tigr42/pysubs/).

## Building etc.

- Use [Leiningen 2](https://github.com/technomancy/leiningen) (tested with 2.0.0-preview10).
- ClojureSubs is meant to be ClojureScript compatible and can be built via [lein-cljsbuild](https://github.com/emezeske/lein-cljsbuild).

## Usage

```
(:use clojuresubs.core)
(-> "1
00:00:20,000 --> 00:00:24,400
Altocumulus clouds occur between six thousand

2
00:00:24,600 --> 00:00:27,800
and twenty thousand feet above ground level."
  parse-subrip
  (shift 1000) ;; milliseconds
  serialize-subrip
  print)
```
