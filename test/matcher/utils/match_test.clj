(ns matcher.utils.match-test
  (:require [midje.sweet :refer :all]
            [matcher.utils.match :as utils]
            [matcher.matchers :as m]
            [clojure.core.unify :as u]))

(def foo 10)
(fact "parses args correctly from matchers"
  (utils/parse-args '(m/list ?arg _ 10 foo)) => '(m/list '?arg '_ 10 foo)
  (utils/parse-args '(m/list ?arg _ (m/list ?b) foo)) => '(m/list '?arg '_ (m/list '?b) foo))

(def test-list (list 1 2 3))
(facts "parses match args correcly"
  (utils/apply-match test-list (m/list 1 2 3)) => [[1 2 3] [1 2 3]]
  (utils/apply-match test-list (m/list '?a 2 3)) => [['?a 2 3] [1 2 3]]
  (utils/apply-match test-list (m/list 1 2)) => nil)

(def inner-list (list 1 (list 2 3) 4))
(facts "parses inner match args correctly"
  (utils/apply-match inner-list (m/list 1 (m/list 2 3) 4))
  => [[1 [2 3] 4] [1 [2 3] 4]]
  (utils/apply-match inner-list (m/list '?a (m/list '?a '?b) 4))
  => [['?a ['?a '?b] 4] [1 [2 3] 4]]
  (utils/apply-match test-list (m/list 1 (m/list 2 3) 4)) => nil)

(facts "matching patterns"
  (background
    (gensym) => 'foo)

  (fact "adds an let if there are unbound vars"
    (utils/wrap-let 'test-list '(m/list 10 (m/list 20) 30) ':foo)
    => `(if-let [~'foo (some->> (utils/apply-match ~'test-list (~'m/list 10
                                                                         (~'m/list 20)
                                                                         30))
                                (apply u/unify))]
         :foo)

    (utils/wrap-let 'test-list '(m/list ?a (m/list ?a ?b) _) '(+ a b))
    => `(if-let [~'foo (some->> (utils/apply-match ~'test-list (~'m/list ~''?a
                                                                         (~'m/list ~''?a ~''?b)
                                                                         ~''_))
                                (apply u/unify))]
          (let [~'a (~''?a ~'foo)
                ~'b (~''?b ~'foo)]
            (~'+ ~'a ~'b)))))

(facts "pattern matching without macros"
  (fact "expands macro in a sane way"
    (utils/match* 'test-list
      '(m/list 10) :foo)
    => `(if-let [f ..first-clause..]
          :foo
          (throw (IllegalArgumentException. "No match")))
    (provided
      (utils/wrap-let 'test-list '(m/list 10) :foo) => `(if-let [f ..first-clause..] :foo)))

  (fact "expands macro with two matches"
    (utils/match* 'test-list
      '(m/list 10) :foo
      '(m/list 20) :bar)
    => `(if-let [f ..first-clause..]
          :foo
          (if-let [s ..second-clause..]
            :bar
            (throw (IllegalArgumentException. "No match"))))
    (provided
      (utils/wrap-let 'test-list '(m/list 10) :foo) => `(if-let [f ..first-clause..] :foo)
      (utils/wrap-let 'test-list '(m/list 20) :bar) => `(if-let [s ..second-clause..] :bar))))
  ;     '((m/list 1 2) :foo
  ;       (m/list (m/list ?a) ?b)))
  ;   => `(cond
  ;         (utils/apply-match '(m/list '?a)))))
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
