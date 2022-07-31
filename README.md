# sertive

Misadventures in error throwing and handling.

## Goal

Encourage exception standardization in Clojure/Script.

## Getting Started

### Installation

***deps.edn:***

```clojure
{:paths ["src"]
 :deps {io.github.timothypratley/sertive {:git/sha "latest"}}}
```
### Usage

```clojure
(ns my.ns
  (:require [sertive.core :as ser]))

(defn foo [x]
  (let [y (rand-int x)]
    (when (even? y)
      (ser/fail ::unlucky-rng))))

(foo 100)
;=> Exception: unlucky-rng {x 100, y 42}
```

## Rationale

### Prelude

In the beginning there were Exceptions.
We complained, for the interop was uncomfortable.
And so it was that Rich brought forth ex-info.
And it soothed our pain, and all was well.

### Problem

Exceptions come in all shapes and sizes.
The benefit of ex-info is some regularity in the form of there shall be a message and a data map.
The curse remains that there is no standardization on what should go into the string or the data.
This makes examining, logging, summarizing and responding to exceptions a task that requires knowing everywhere, how, what and why about where exceptions might occur.
Conventions are not shared between library creators, leading to a mishmash of exception shapes.

The problem space includes:

* Handling Java exceptions that might make it to user level code
* Handling Clojure ex-info exceptions that might make it to user level code
* Raising an ex-info under exceptional circumstances
* Specifying precondition metadata on functions
* Making assertions (a) that should only occur in debug (b) that should always occur

### Solution

1. Provide a new ex-info constructor that conforms to a standard. Must have an id, may have tags, must indicate what scope is of particular interest, automatically captures that scope and possibly more in data, encouraged to have a suggestion.
2. Provide a standard mechanism for message string construction.
3. Provide a macro to wrap a block of code in a try-catch-release form that will associate extra information to that stack frame. For example contextual information about what the user was trying to do, or an alternative display name, capture additional scope, add higher level suggestions, add new tags, add extra data.
4. Provide a modified fn syntax that makes (3) syntactically part of defining the function and replace pre- and post-conditions with ex-info that conforms to the standard.
5. Provide an assert that (a) can choose to always run, not only in debug, (b) captures the ex-info that conforms to the standard

### Principles

Design choices are guided by the following fundamental propositions

***Table 1: Implementation design choice principles***

| Value            | Implication                                                                                                                |
|------------------|----------------------------------------------------------------------------------------------------------------------------|
| **Transparency** | Anything contained in the message string should exist in the ex-data                                                       |
| **Ubiquity**     | Any minor convention accepted as standard and admitted to core would be more valuable than a perfect solution as a library |
| **Accretive**    | Layers of my program should be able to add additional information into the stacktrace.                                     |

### Ideas
* **Unique ID:** have a required id associated with raising an ex-info; id should be package qualified, and namespaced, and universally unique (only raised in one place in your code, and does not collide with other libraries).
* **Tags:** Analogous to “ArithmeticException” superclass. Optional set of package qualified, namespaced identifiers. Optionally provide a hierarchy. Provide a standard set of base error categories and allow extension.
* **Derive Messages:** No string needed. Construct message from the id and an expression that captures anything important in scope and renders it into a string. Avoid string templating and functions in favor of simpler things like prn-str of a vector.
* **Suggestion:**  Have an optional but strongly encouraged suggestion associated with raising an ex-info. Exceptions are strongest when they provide (1) exactly what went wrong and (2) A suggestion on what the observer can do about it. For example: retry?
* **Summarizing data:** For example if a key is not in a map, it’s nice to see the map. But it might be too big. Logic: if nil or empty, just print it, if can display the keys in less than 40 characters show them, else show a key count.
* **Explain data:** There should be a convenient way to express “key :foo is missing from m: {}”. It seems like there is a limited number of useful primitives; looking up values in maps, sets, using things that are in scope or out of scope, comparing numbers, size of collections, and doing boolean logic. Ideally construct these easier than Clojure.spec (or keep basic enough that the explaination makes sense, and they are obvious to create).

## Challenges

* If exceptions need to be package and namespace qualified, how do we know the package? Is the id too long? Would people be better served with a UUID or hash?

## License

Copyright © 2014 Timothy Pratley

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
