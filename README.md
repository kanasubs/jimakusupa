jimakusupa
==========
Jimakusupa is a fork from [clojuresubs](https://github.com/tigr42/clojuresubs) wrapping [subtitleConverter](https://github.com/JDaren/subtitleConverter) for some
features that clojuresubs doesn't support. If you want
to process subtitle files in ClojureScript, use clojuresubs instead.

### Installing
-------
[![clojars version](https://clojars.org/jimakusupa/latest-version.svg?raw=true)](https://clojars.org/jimakusupa)

### Crafting
-------
```clj
user=> (use 'jimaku.supa)
user=> (def subs "1
00:00:20,000 --> 00:00:24,400
Altocumulus clouds occur between six thousand

2
00:00:24,600 --> 00:00:27,800
and twenty thousand feet above ground level.")
user=> (-> (parse :srt subs) (shift 1000) serialize-subrip print) ; miliseconds
;=>1
  ;00:00:21,000 --> 00:00:25,400
  ;Altocumulus clouds occur between six thousand
  ;
  ;2
  ;00:00:25,600 --> 00:00:28,800
  ;and twenty thousand feet above ground level.
```

### Goals
-------
1. Wrap subtitleconvert for everything that clojuresubs doesn't
support.
2. The wrapped forms will be gradually replaced with Clojure implementations,
until subtitleconvert is completely removed.
3. Finally, jimakusupa will be ported to ClojureScript

### Features
-------
- Read and write SubRip (``*.srt``) files
- Shift by given amount of time
- Read SubStation Alpha (``*.ass``, ``*.ssa``) files

### Wishlist
-------
- Write SubStation Alpha (``*.ass``, ``*.ssa``) files
- Port to ClojureScript

### Missing something or you just don't agree?
-------
Ask me.
Pull requests are welcome!

### Using jimakusupa in Ramen or Disruptive projects?
-------
I'll be happy to add your project using jimakusupa to this list.

[kanasubs.com](http://www.kanasubs.com) — Convert raw subtitles in Kanji to Kana online.

### Gentle contributions
-------
- [tigr42](https://github.com/tigr42)
- [Carlos Cunha](https://github.com/ccfontes)

### License
-------
Copyright (C) 2014 Carlos C. Fontes.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
