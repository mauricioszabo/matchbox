# Matcher

[![Clojars Project](https://img.shields.io/clojars/v/matcher.svg)](https://clojars.org/matcher)

Matcher is an extensible Clojure pattern matching library. It intends to be simple, extensible, powerful, and idiomatic - for example, `match` syntax is almost the same of `cond`, so no surprises here.

The important keywords are **simple** - _matcher_ depends on `core.unify`, so all variable bindings are very simple, and all matchers are just curried functions, so they are partially evaluated every time, so no magic - and **extensible** - there are _no_ "built-in matchers - everything must be or a single value or a function. No strange huge and un-debuggable macros to deconstruct matchers.

## Usage

The core of everything is on `matcher.matches` namespace. Each match clause is a value, or a function that receives a single argument (more on that later).

```clojure
(require '[matcher.matches :as m])

(defn example [some-obj]
  (m/match some-obj
    10 "some-obj is ten"
    (m/list ?a & _) (str "it is a list, and first arg is " a)
    string? "It is a string"
    _ "I don't know what some-obj is..."))
```

The namespace `matcher.matches` contains a bunch of *matcher functions*. These are special functions that will check specific rules and bind variables if matched.

```clojure
(m/match some-obj
  (m/list ?first ?second 3) (+ first second))
```

The code above will try to match a list (it will **not** match vectors or maps) that have three elements, and the third is `3`. It will bind the first element to the local variable `first` and will bind the second variable to `second`.

More examples can be found on the `examples` directory.

## Matcher Functions

Any function that receive only one argument, and return true or false, can be used as a matcher. So, `odd?`, `even?`, `map?` and such can be used as matchers, but they _will not_ bind variables nor check for exceptions. So, the code below will throw an error because `"10"` is not a number:

```clojure
(m/match "10"
  odd? "It's a wild world...")
```

To avoid exceptions and bind variables to the match functions, it is better to use a specific _matcher function_. One of these functions is `satisfies` that will wrap the predicate in a try-catch, and it'll allow us to capture the result of predicate. The code below, for instance, returns `40`, the result of the only `some` that matches (the second one):

```clojure
(m/match '(10 20 30 40 50)
  (m/satisfies #(some #{-1 -2} %) ?r) (str "Negative number found: " r)
  (m/satisfies #(some #{3 4 50 40} %) ?r) (str "Positive number found: " r))
```

Please notice that there are **no** built-in matchers - just Clojure functions. `satisfies`, for instance, is just a function that returns another function, wrapping the check in a try-catch block. You could, for instance, check what `satisfies` returns by just running it by hand (and quoting `?r` and other symbols):

```clojure
(m/satisfies #(some #{3 4 50 40} %) '?r) ; => returns a function
((m/satisfies #(some #{3 4 50 40} %) '?r) '(10 20 30 40 50))
; => returns ['?r 40]
```

On `matcher.matchers` namespace, there are already a bunch of matchers prepared:

* `list`, `vector`, and `coll` - matchers for collections. To correcly match, you need to either provide each individual element or use `&` to match the rest of the list. For example `(list)` will match any empty list (but not vector), `(vector 10 & _)` matches any vector that begins with `10`, and `(coll 10 20)` will match either a list or vector with two elements, `10` and `20`.
* `matches` is a generic unifier. It'll just call `core.unify/unify`. This matcher is *not* composable: you cannot use `(matches odd?)` for example. In general, avoid this match, unless you want to match maps or map-like structures like records, for instance, and don't want the extra overhead introduced by `map` matcher.
* `satisfies` wrap a predicate (like `odd?`, `even`, etc) in a try-catch, so that they will not throw exceptions if called with wrong arguments. Furthermore, it allows another parameter that will try to unify with the predicate's result.
* `map` is a complicated matcher. It'll try to match keys *and* values on a map. It'll accept an even number of arguments, and will lazily check each key and value to unify it. Of course, as maps' keys are unordered, this could mean that unified keys are not previsible. For example, `(map ?a 10)`, applied to `{:a 10 :b 10 :c 10}`, could bind `a` to `:a`, `:b` or `:c`
* `regexp` asks for a regular expression, and a number of other arguments. If the regexp have capture groups, other arguments will be the capture groups, in order. So, if they match, the matcher will be successful. If they are unbound variables, they'll be bound to the regular expression's capture groups. There must be the same number of arguments that are capture groups, or none at all.
* `instance` simply checks if object is an instance of a specific parameter. For example, trying to match a list, `(m/instance clojure.lang.PersistentList)` will match, and `(m/instance ?c)` will bind `c` to `clojure.lang.PersistentList`
* `record` checks if object is a specific record, and tries to unify its arguments (in order of definition). See examples for more info.

## Custom matchers

## License

Copyright © 2016 Maurício Szabo

Distributed under the Eclipse Public License either version 1.0 or any later version.
