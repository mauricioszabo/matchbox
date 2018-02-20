(ns matcher.matchers-test
  (:require [midje.sweet :refer :all]
            [matcher.matchers :as m]
            [matcher.utils.match :as utils]))

(defn match-and-unify [ & {:as map}]
  (fn [obj]
    (= map (utils/unify obj))))

(defn dont-match [obj]
  (or (nil? obj) (utils/unify obj)))

(defn match [obj]
  (= {} (utils/unify obj)))

(def test-list (list 1 2 3))
(fact "Matching against lists"
  ((m/list 1 2 3) [1 2 3]) => dont-match
  ((m/list 1 2 3) test-list) => match
  ((m/list 1 '?foo 3) test-list) => (match-and-unify '?foo 2)
  ((m/list 1 '& '?rest) test-list) => (match-and-unify '?rest '(2 3)))

(def test-vec [1 2 3])
(fact "Matching against vectors"
  ((m/vector 1 2 3) '(1 2 3)) => nil
  ((m/vector 1 2 3) test-vec) => [[1 2 3] [1 2 3]]
  ((m/vector 1 '?foo 3) test-vec) => [[1 '?foo 3] [1 2 3]]
  ((m/vector 1 '& '?rest) test-vec) => [[1 '?rest] [1 '(2 3)]])

(fact "Matching against any collection"
  ((m/coll 1 2 3) '(1 2 3)) => match
  ((m/coll 1 '?foo 3) test-vec) => (match-and-unify '?foo 2))

(def test-map {:a 10, :b 20})
(fact "Matching using unifier only"
  ((m/matches {:a '?a, '?b 20}) test-map) => [{:a '?a, '?b 20} {:a 10 :b 20}]
  ((m/matches {:a '?a, '?b 20}) test-vec) => [{:a '?a, '?b 20} [1 2 3]]
  ((m/matches [1 2 3]) test-vec) => [[1 2 3] [1 2 3]])

(facts "Predicate matching with core functions"
  ((m/satisfies even?) 10) => match
  ((m/satisfies even? '?result) 10) => (match-and-unify '?result true)
  ((m/satisfies even? '?result) "FOO") => dont-match)

(facts "Matching against maps"
  (fact "simple matches"
    ((m/map :a '?a) test-vec) => dont-match
    ((m/map) test-map) => match
    ((m/map :a '?a) test-map) => (match-and-unify '?a 10)
    ((m/map '?a 20) test-map) => (match-and-unify '?a :b)
    ((m/map '?a '?b) test-map) => (match-and-unify '?a :a '?b 10)
    ((m/map '?a 20 '?b 10) test-map) => (match-and-unify '?a :b '?b :a))

  (fact "composite matches"
    ((m/map :a (m/list)) test-map) => dont-match
    ((m/map :a 10 :b (m/list)) test-map) => dont-match
    ((m/map '?a (m/list '& '_)) {:a 10, :b 20, :c '(1 2 3)}) => (match-and-unify '?a :c)))

(fact "Matching strings"
  (fact "matches regexp"
    ((m/regexp #"(.*)@(.*)" "bar" '?b) "foo@bar") => dont-match
    ((m/regexp #"(.*)@(.*)") "foo@bar") => match
    ((m/regexp #"(.*)@(.*)" '?a '?b) "foo@bar") => (match-and-unify '?a "foo" '?b "bar")))

(fact "Matching instances of some element"
  ((m/instance clojure.lang.PersistentList) '(1 2) ) => []
  ((m/instance clojure.lang.PersistentList) [1 2] ) => dont-match
  ((m/instance 10) [1 2] ) => dont-match)

(defrecord Example [a b c])
(defrecord Example2 [a b c])
(def test-rec (->Example 10 20 30))
(fact "Matching records"
  ((m/record Example '?a '?b) test-rec) => dont-match
  ((m/record Example2 '?a '?b '?c) test-rec) => dont-match
  ((m/record Example '?a '?b '?c) test-rec) => (match-and-unify '?a 10 '?b 20 '?c 30)
  ((m/record Example 10 '?b '?c) test-rec) => (match-and-unify '?b 20 '?c 30)
  ((m/record Example) test-rec) => match

  ((m/record 'Example3 '?a '?b '?c) test-rec) => dont-match
  ((m/record java.lang.Byte '?a '?b '?c) test-rec) => dont-match
  ((m/record matcher.matchers_test.Example '?a '?b '?c) test-rec)
  => (match-and-unify '?a 10 '?b 20 '?c 30))

(facts "boolean matches"
  (fact "or applies first rule that matches"
    ((m/or (m/list 1 '?l 3) (m/list 1 '?v 3)) test-vec) => dont-match
    ((m/or (m/list 1 '?l 3) (m/vector 1 '?v 3)) test-vec) => (match-and-unify '?v 2)
    ((m/or (m/vector '?first 2 3) (m/vector 1 '?v 3)) test-vec) => (match-and-unify '?first 1))

  (fact "and applies only if all rules apply"
    ((m/and (m/vector 1 '?l 3) (m/list 1 1 3)) test-vec) => dont-match
    ((m/and (m/vector 1 '?v 3) (m/vector 1 2 '?v)) test-vec) => dont-match
    ((m/and (m/vector 1 '?v1 '?l) (m/vector 1 '?v2 '?l)) test-vec)
    => (match-and-unify '?v1 2 '?v2 2 '?l 3)))
    ; ((m/and (m/vector '?first 2 3) (m/vector 1 '?v 3)) test-vec) => (match-and-unify '?first 1)))

(facts "Using match macro"
  (fact "matching literal values"
    (m/match 10
      10 :foo
      20 :bar) => :foo)

  (fact "matching predicate fns"
    (m/match 20
      odd? :is-odd
      even? :is-even) => :is-even)

  (fact "matching without vars"
    (m/match test-list
      (m/list 1 2 1) :foo
      (m/list 1 2 3) :bar) => :bar

    (m/match '(1 2 (2 3) 4)
      (m/list 1 2 '(2 2) 4) :foo
      (m/list 1 2 (m/list 2 3) 4) :bar) => :bar)

  (fact "matching with vars"
    (m/match test-list
      (m/list ?a 2 ?a) :foo
      (m/list ?a 2 ?b) (+ a b)) => 4

    (m/match '(1 2 (2 3) 4)
      (m/list ?a 2 (m/list ?a ?a) 4) :foo
      (m/list 1 ?a (m/list ?a ?b) ?c) (+ a b c)) => 9)

  (fact "applies inner params and anon functions"
    (m/match '(30 40 50)
      (m/matches {:a ?b}) :foo
      (m/satisfies #(some #{-1 -2} %) ?r) r
      (m/satisfies #(some #{3 4 50 40} %) ?r) r) => 40))
