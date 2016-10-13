(ns matcher.matchers-test
  (:require [midje.sweet :refer :all]
            [matcher.matchers :as m]))

(def test-list (list 1 2 3))
(fact "Matching against lists"
  ((m/list 1 2 3) [1 2 3]) => nil
  ((m/list 1 2 3) test-list) => [[1 2 3] [1 2 3]]
  ((m/list 1 '?foo 3) test-list) => [[1 '?foo 3] [1 2 3]]
  ((m/list 1 '& '?rest) test-list) => [[1 '?rest] [1 '(2 3)]])

(def test-vec [1 2 3])
(fact "Matching against vectors"
  ((m/vector 1 2 3) '(1 2 3)) => nil
  ((m/vector 1 2 3) test-vec) => [[1 2 3] [1 2 3]]
  ((m/vector 1 '?foo 3) test-vec) => [[1 '?foo 3] [1 2 3]]
  ((m/vector 1 '& '?rest) test-vec) => [[1 '?rest] [1 '(2 3)]])

(def test-map {:a 10, :b 20})
(fact "Matching using unifier only"
  ((m/matches {:a '?a, '?b 20}) test-map) => [{:a '?a, '?b 20} {:a 10 :b 20}]
  ((m/matches {:a '?a, '?b 20}) test-vec) => [{:a '?a, '?b 20} [1 2 3]]
  ((m/matches [1 2 3]) test-vec) => [[1 2 3] [1 2 3]])

(facts "Matching against maps"
  (fact "simple matches"
    ((m/map :a '?a) test-vec) => nil
    ((m/map) test-map) => [[] []]
    ((m/map :a '?a) test-map) => [[[[] ['?a]]] [[[] [10]]]]
    ((m/map '?a 20) test-map) => [[[['?a] []]] [[[:b] []]]]
    ((m/map '?a '?b) test-map) => [[[['?a] ['?b]]] [[[:a] [10]]]]
    ((m/map '?a 20 '?b 10) test-map)
    => [[[['?a] []] [['?b] []]] [[[:b] []] [[:a] []]]])

  (fact "composite matches"
    ((m/map :a (m/list)) test-map) => nil
    ((m/map :a 10 :b (m/list)) test-map) => nil
    ((m/map '?a (m/list '& '_)) {:a 10, :b 20, :c '(1 2 3)})
    => [[[['?a] []]] [[[:c] []]]]))

(fact "Matching instances of some element"
  ((m/instance? clojure.lang.PersistentList) '(1 2) ) => []
  ((m/instance? clojure.lang.PersistentList) [1 2] ) => nil
  ((m/instance? 10) [1 2] ) => nil)

(facts "Using match macro"
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
      (m/list 1 ?a (m/list ?a ?b) ?c) (+ a b c)) => 9))
