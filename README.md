# Matcher

[![Clojars Project](https://img.shields.io/clojars/v/matcher.svg)](https://clojars.org/matcher)

Matcher is an extensible Clojure pattern matching library. It intends to be simple, extensible, powerful, and idiomatic - for example, `match` syntax is almost the same of `cond`, so no surprises here.

The important keywords are:

* **simple** - _matcher_ uses the same format as `core.unify`, so variable bindings are
the same as unify (?a, ?b, etc), and all matchers are just curried functions, so they
are partially evaluated every time (no magic involved)

* **extensible** - there are _no_ "built-in matchers - everything must be or a single
value or a function. No strange, huge and un-debuggable macros to deconstruct matchers.

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

The code above will try to match a list (it will **not** match vectors or maps) that have
three elements, and the third is `3`. It will bind the first element to the local variable
`first` and will bind the second variable to `second`.

More examples can be found on the `examples` directory.

## Matcher Functions

Any function that receive only one argument, and return true or false, can be used as a
matcher. So, `odd?`, `even?`, `map?` and such can be used as matchers, but they _will not_
bind variables nor check for exceptions. So, the code below will throw an error because
`"10"` is not a number:

```clojure
(m/match "10"
  odd? "It's a wild world...")
```

To avoid exceptions and bind variables to the match functions, it is better to use a
specific _matcher function_. One of these functions is `satisfies` that will wrap the
predicate in a try-catch, and it'll allow us to capture the result of predicate. The code
below, for instance, returns `40`, the result of the only `some` that matches (the second
one):

```clojure
(m/match '(10 20 30 40 50)
  (m/satisfies #(some #{-1 -2} %) ?r) (str "Negative number found: " r)
  (m/satisfies #(some #{3 4 50 40} %) ?r) (str "Positive number found: " r))
```

Please notice that matchers are **not** special in any way - they're just Clojure
functions. `satisfies`, for instance, is just a function that returns another function,
wrapping the check in a try-catch block. You could, for instance, check what `satisfies`
returns by just running it by hand (and quoting `?r` and other symbols):

```clojure
(m/satisfies #(some #{3 4 50 40} %) '?r) ; => returns a function
((m/satisfies #(some #{3 4 50 40} %) '?r) '(10 20 30 40 50))
; => returns ['?r 40]
```

On `matcher.matchers` namespace, there are already a bunch of matchers prepared (with examples):

* `list`, `vector`, and `coll` - matchers for collections. To correcly match, you need to
either provide each individual element or use `&` to match the rest of the list. For
example `(list)` will match any empty list (but not vector), `(vector 10 & _)` matches
any vector that begins with `10`, and `(coll 10 20)` will match either a list or vector
with two elements, `10` and `20`.

```clojure
(m/match [1 2 3 4]
  (m/list 1 2 3 4) "won't match - its not a list"
  (m/vector 1 2 3) "won't match - missing one element"
  (m/vector 1 ?a ?a 4) "won't match - second and third elements are not equal"
  (m/vector 1 2 3 4) "will match"
  (m/vector 1 _ & ?rest) "would match, but previous matcher catch the code")
```
* `matches` is a generic unifier. It'll just call `core.unify/unify`. This matcher is *not* composable: you cannot use `(matches odd?)` for example. In general, avoid this match, unless you want to match maps or map-like structures like records, for instance, and don't want the extra overhead introduced by `map` matcher.
```clojure
(m/match '(1 2 3 4)
  (m/matches [1 2 ?a ?a]) "will not match - third and fourth are not equal"
  (m/matches (m/vector 1 2 3 4)) "will not match - matches does not allow inner matchers"
  (m/matches [1 2 3 4]) "will match - matches uses unify, which does not differentiate colls")
```
A possible use would be to match maps faster:
```clojure
(m/match {:a 10 :b 20}
  (m/matches {:c ?a}) "will not match - there's no :c in map"
  (m/matches {?a 10}) "will not match - missing :b"
  (m/matches [[?a 10] & _]) "will match, because it treats all colls the same")
```

* `satisfies` wraps a predicate (like `odd?`, `even`, etc) in a try-catch, so that they will not throw exceptions if called with wrong arguments. Furthermore, it allows another parameter that will try to unify with the predicate's result. `satisfies` does not accept inner matches - so, something like `(satisfies to-int odd?)` will not work.
```clojure
(m/match "10"
  (m/satisfies even?) "will not match - it's not a number"
  ; Please, notice that parseInt need to be in anon function because Clojure
  ; can't identify it as a function (because it's Java interop):
  (m/satisfies #(Integer/parseInt %) ?n) (str "will match, and the number is: " n))
```

* `map` is a complicated matcher. It'll try to match keys *and* values on a map. It'll accept an even number of arguments, and will lazily check each key and value to unify it. Of course, as maps' keys are unordered, this could mean that unified keys are not previsible. For example, `(map ?a 10)`, applied to `{:a 10 :b 10 :c 10}`, could bind `a` to `:a`, `:b` or `:c`
```clojure
(m/match {:some "strange" :map "Here"}
  (m/map :some number?) "Will not match - value of :some is not a number"
  (m/map :missing _) "Will not match - there's not :missing key"
  (m/map :some) "Will not match - missing value for :some key"
  (m/map :some string?) "Will match")
```
* `regexp` asks for a regular expression, and a number of other arguments. If the regexp have capture groups, other arguments will be the capture groups, in order. So, if they match, the matcher will be successful. If they are unbound variables, they'll be bound to the regular expression's capture groups. There must be the same number of arguments that are capture groups, or none at all.
```clojure
(m/match "github.com/mauricioszabo/matcher"
  (m/regexp #".*/(.*)/(.*)" ?a ?a) "Will not match - two parts of URL are not the same"
  (m/regexp #".*/(.*)/(.*)" _ "clj") "Will not match - second part of URL is not right"
  (m/regexp #".*/(.*)/(.*)" ?a "matcher") (str "Will match, and capture " a)
  (m/regexp #".*/(.*)/(.*)") "Would match, but previous matcher already captured")
```
* `instance` simply checks if object is an instance of a specific parameter. For example, trying to match a list, `(m/instance clojure.lang.PersistentList)` will match, and `(m/instance ?c)` will not match (it doesn't makes sense to bind variables to something we could just use `(type ..some-obj..)`.
```clojure
(m/match "Foo Bar"
  (m/instance Integer) "Will not match - it's not an Integer"
  (m/instance 'String) "Will not match - it's not a class"
  (m/instance String) "Will match")
```

* `record` checks if object is a specific record, and tries to unify its arguments (in order of definition). See examples for more info.
```clojure
(defrecord RGB [r g b])
(defrecord RGBA [r g b a])
((m/record RGB) (->RGB 10 20 30))
(m/match (->RGB 10 20 30)
  (m/record RGBA _ _ _ _) "Will not match - it's not a RGBA"
  (m/record RGB ?a ?a _) "Will not match - r and g are different"
  (m/record RGB 10 ?g 30) (str "Will match and capture green: " g))
```

## Custom matchers
One of the core ideas for Matcher is to be extensible - so, it's possible to define your own matcher on it.

A matcher is simply a function that, when applied to the matching object, will return truthy or falsey. If you want to create a matcher that'll bind variables, you **need** to return a vector with exactly two elements. The "left side" is the parameters passed to test, if any; the "right side" is the matching part. This will be passed to `clojure.core.unify/unify`, and if that returns a map, it'll be a match. If the map have unbound variables (variables that starts with `?`, like `?foo`), let-variables will be created.

One other thing is that if you want matcher to recurse into results (that is, if we want Matcher to test inner matchers), then we must assure that the left-side and right-side are colls (vectors, lists, maps or sets).

For example, let's create a matcher that will ask what's the previous prime of that number, if any. First of all, remember that what the matcher will receive can be an integer, or not - so, we need to check for this too. Second, remember that the user can bind a variable on it, or can just check if the last-prime is a specific number (but `unify` will check for us, so we don't need to check for it).

So, we'll define a function named `last-prime` that will expect an argument, and it'll return another function. This other function will: (1), check if the parameter passed to it is an integer; (2), check if the number is greater than `2` (because there's no primes before `2`), and (3), return a vector with what we expect the last-prime to be, and what the last-prime really is, wrapped in colls (first in a vector, second in a list):

```clojure
(defn last-prime [number]
  (fn [possible-number]
    (when (integer? possible-number)
      [[number]
       (->> (range possible-number 2 -1)
            (remove (fn [x] (some #(-> x (rem %) zero?) (range 2 x))))
            (take 1))])))
```

Simple code, line 5 to 7 calculates the prime, and assigns it to the second element of the vector. The first argument of the vector is just the number we expect. If the two numbers are different, `unify` will take care for us. If the first number is an unbound variable, it'll bind the variable for us. And, if the first argument is another matcher, then **Matcher** will take care for us. So, we can just use the matcher:

```clojure
(m/match 10
  (last-prime 9) "Will not match - 9 is not even prime!"
  (last-prime 5) "Will not match - it is not 5"
  (last-prime even?) "Will not match - it's odd"
  (last-prime odd?) "Will match - it's odd")
```

If we remove the "vector" over the number (line 4) and change `(take 1)` to `first`, we'll see that the matcher still works, but it'll not recurse into inner matches (like `odd?` or `even?` - these lines would always return false, and the code above would only check for unbound vars or numbers.

### Custom and composite matchers
There are matchers (like `and` and `or`) that need to know the result from previous matchers to be able to decide if they match or not. Let's see how to create a `not` matcher, one that negates the result of current matcher (please note: because of the way matchers work, this is _really_ a bad idea - unbound variables will all be bound to `nil`, for instance, but this is only an example).

If we look at `last-prime`, for example, we can see that only running the matcher's function is not sufficient to check if there was a match or not. So, we need to import `matcher.utils.match`, a bunch of utility functions that recursively checks for matches:

```clojure
(require '[matcher.utils.match :as utils])
(utils/match-and-unify 10 (last-prime 9)) ; => nil
(utils/match-and-unify 10 (last-prime 7)) ; => {}
(utils/match-and-unify 10 (last-prime '?n)) ; => {'?n 7}
```

So, a `not` matcher would be just:

```clojure
(require '[matcher.utils.match :as utils])
(defn not-matcher [matcher]
  (fn [obj]
    (when (not (utils/match-and-unify obj matcher))
      true)))

(m/match [1 2 3]
  (not-matcher (m/vector 1 2 3)) "Will not match"
  (not-matcher (m/list :foo :bar)) "Match!")
```

For more information, look at how `or` and `and` matchers are implemented.

## License

Copyright © 2016 Maurício Szabo

Distributed under the Eclipse Public License either version 1.0 or any later version.
