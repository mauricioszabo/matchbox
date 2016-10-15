# Matchbox

Matchbox is an extensible Clojure pattern matching library. It intends to be simple, extensible, powerful, and idiomatic - for example, `match` syntax is almost the same of `cond`, so no surprises here.

## Usage

The core of everything is on `matchbox.matches` namespace. Each match clause is a value, or a function that receives a single argument (more on that later).

```clojure
(require '[matchbox.matches :as m])

(defn example [some-obj]
  (m/match some-obj
    10 "some-obj is ten"
    (m/list ?a & _) (str "it is a list, and first arg is " a)
    str? "It is a string"
    _ "I don't know what some-obj is...")
```

The namespace `matchbox.matches` contains a bunch of *matcher functions*. These are special functions that will check specific rules and bind variables if matched.

```clojure
(m/match some-obj
  (m/list ?first ?second 3) (+ first second))
```

The code above will try to match a list (it will **not** match vectors or maps) that have three elements, and the third is `3`. It will bind the first element to the local variable `first` and will bind the second variable to `second`.

## Matchers


## License

Copyright © 2016 Maurício Szabo

Distributed under the Eclipse Public License either version 1.0 or any later version.
