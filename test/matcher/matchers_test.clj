(ns matcher.matchers-test
  (:require [midje.sweet :refer :all]
            [matcher.matchers :as m]))

(def test-list (list 1 2 3))
(fact "Matching against lists"
  ((m/list 1 2 3) [1 2 3]) => nil
  ((m/list 1 2 3) test-list) => [[1 2 3] [1 2 3]]
  ((m/list 1 '?foo 3) test-list) => [[1 '?foo 3] [1 2 3]]
  ((m/list 1 '& '?rest) test-list) => [[1 '?rest] [1 '(2 3)]])

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
