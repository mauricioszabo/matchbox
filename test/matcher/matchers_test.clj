(ns matcher.matchers-test
  (:require [midje.sweet :refer :all]
            [matcher.matchers :as m]))

(def test-list (list 1 2 3))
(subvec (vec test-list) 0 1)
(fact "Matching against lists"
  ((m/list 1 2 3) [1 2 3]) => nil
  ((m/list 1 2 3) test-list) => [[1 2 3] [1 2 3]]
  ((m/list 1 '?foo 3) test-list) => [[1 '?foo 3] [1 2 3]]
  ((m/list 1 '& '?rest) test-list) => [[1 '?rest] [1 '(2 3)]])

(facts "about match*"
  (background
    (gensym) => 'foo)

  (fact ""))

  ; (fact "combines results and matches then"
  ;   (macroexpand-1 '(m/match test-list
  ;                     (m/list 1 2 3) :foo))
  ;   => `(if-let [~'foo (~'m/list ~'test-list 1 2 3)]
  ;         :foo
  ;         (throw (IllegalArgumentException. "No match")))))

(fact "extracting values for pattern-matching"
  (m/extract-vals test-list (m/list 1 2)) => nil
  (m/extract-vals test-list (m/list 1 2 3)) => {:vals [[1 1] [2 2] [3 3]]}
  (m/extract-vals test-list (m/list ?b ?a 3)) => {:bindings [['b 1]
                                                             ['a 2]]
                                                  :vals [[3 3]]}
  (m/extract-vals test-list (m/list ?b ?a ?b)) => {:bindings [['b 1]
                                                              ['a 2]]
                                                   :vals [['b 3]]})
