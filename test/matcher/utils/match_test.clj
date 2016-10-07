(ns matcher.utils.match-test
  (:require [midje.sweet :refer :all]
            [matcher.utils.match :as utils]
            [matcher.matchers :as m]))

(def foo 10)
(fact "parses args correctly from matchers"
  (utils/parse-args '(m/list ?arg _ 10 foo)) => '(m/list '?arg '_ 10 foo))

(def test-list (list 1 2 3))
(facts "parses match args correcly"
  (utils/apply-match test-list (m/list 1 2 3)) => [[1 2 3] [1 2 3]]
  (utils/apply-match test-list (m/list '?a 2 3)) => [['?a 2 3] [1 2 3]]
  (utils/apply-match test-list (m/list 1 2)) => nil)

(def inner-list (list 1 (list 2 3) 4))
(facts "parses inner match args correctly"
  (utils/apply-match inner-list (m/list 1 (m/list 2 3) 4))
  => [[1 [2 3] 4] [1 [2 3] 4]]
  (utils/apply-match test-list (m/list 1 (m/list 2 3) 4)) => nil)

  ; (utils/apply-match test-list (m/list)))
  ; (background
  ;   (gensym) => 'foo)
  ;
  ; (fact ""))
  ;
  ;   ; (fact "combines results and matches then"
  ;   ;   (macroexpand-1 '(m/match test-list
  ;   ;                     (m/list 1 2 3) :foo))
  ;   ;   => `(if-let [~'foo (~'m/list ~'test-list 1 2 3)]
  ;   ;         :foo
  ;   ;         (throw (IllegalArgumentException. "No match")))))
  ;
  ; (fact "extracting values for pattern-matching"
  ;   (m/extract-vals test-list (m/list 1 2)) => nil
  ;   (m/extract-vals test-list (m/list 1 2 3)) => {:vals [[1 1] [2 2] [3 3]]}
  ;   (m/extract-vals test-list (m/list ?b ?a 3)) => {:bindings [['b 1]
  ;                                                              ['a 2]]
  ;                                                   :vals [[3 3]]}
  ;   (m/extract-vals test-list (m/list ?b ?a ?b)) => {:bindings [['b 1]
  ;                                                               ['a 2]]
  ;                                                    :vals [['b 3]]})
