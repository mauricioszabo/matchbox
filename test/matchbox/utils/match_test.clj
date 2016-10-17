(ns matchbox.utils.match-test
  (:require [midje.sweet :refer :all]
            [matchbox.utils.match :as utils]
            [matchbox.matchers :as m]
            [clojure.core.unify :as u]))

(def foo 10)
(fact "parses args correctly from matchers"
  (utils/parse-args '(m/list ?arg _ 10 foo)) => '(m/list '?arg '_ 10 foo)
  (utils/parse-args '(m/list _ & _foo)) => '(m/list '_ '& _foo)
  (utils/parse-args '(m/list ?arg _ (m/list ?b) foo)) => '(m/list '?arg '_ (m/list '?b) foo)
  (utils/parse-args '(m/list [?b _ #{_ ?a}])) => '(m/list ['?b '_ #{'_ '?a}]))

(def test-list (list 1 2 3))
(facts "parses match args correcly"
  (utils/apply-match test-list (m/list 1 2 3)) => [[1 2 3] [1 2 3]]
  (utils/apply-match test-list (m/list '?a 2 3)) => [['?a 2 3] [1 2 3]]
  (utils/apply-match test-list (m/list 1 2)) => nil

  (fact "parses single value matchers"
    (utils/apply-match nil nil?) => [[] []]
    (utils/apply-match nil nil) => [[nil] [nil]]))

(def inner-list (list 1 (list 2 3) 4))
(facts "parses inner match args correctly"
  (utils/apply-match 10 20)
  => [[20] [10]]
  (utils/apply-match inner-list (m/list 1 (m/list 2 3) 4))
  => [[1 [2 3] 4] [1 [2 3] 4]]
  (utils/apply-match inner-list (m/list '?a (m/list '?a '?b) 4))
  => [['?a ['?a '?b] 4] [1 [2 3] 4]]
  (utils/apply-match test-list (m/list 1 (m/list 2 3) 4)) => nil)

(facts "matching patterns"
  (background
    (gensym) => 'foo)

  (fact "unwraps code if there are no sexps on match side"
    (utils/wrap-let 10 10 :foo :bar)
    => `(if-let [~'foo (utils/match-and-unify 10 10)]
         :foo
         :bar))

  (fact "unwraps code if matcher is a set"
    (utils/wrap-let 10 #{1 10} :foo :bar)
    => `(if-let [~'foo (utils/match-and-unify 10 #{1 10})]
         :foo
         :bar))

  (fact "adds an let if there are unbound vars"
    (utils/wrap-let 'test-list '(m/list 10 (m/list 20) 30) ':foo ':bar)
    => `(if-let [~'foo (utils/match-and-unify ~'test-list (~'m/list 10
                                                                    (~'m/list 20)
                                                                    30))]
         :foo
         :bar)

    (utils/wrap-let 'test-list '(m/list ?a (m/list ?a ?b) _) '(+ a b) ':bar)
    => `(if-let [~'foo (utils/match-and-unify ~'test-list (~'m/list ~''?a
                                                                    (~'m/list ~''?a ~''?b)
                                                                    ~''_))]
          (let [~'a (~''?a ~'foo)
                ~'b (~''?b ~'foo)]
            (~'+ ~'a ~'b))
          :bar)))

(facts "pattern matching without macros"
  (fact "expands macro in a sane way"
    (utils/match* 'test-list
      '(m/list 10) :foo)
    => `(if-let [f ..first-clause..]
          :foo
          (throw (IllegalArgumentException. "No match")))
    (provided
      (utils/wrap-let 'test-list '(m/list 10) :foo `(throw (IllegalArgumentException. "No match")))
      => `(if-let [f ..first-clause..] :foo (throw (IllegalArgumentException. "No match")))))

  (fact "expands macro with two matches"
    (utils/match* 'test-list
      '(m/list 10) :foo
      '(m/list 20) :bar)
    => `(if ..c1.. ..10.. (if ..c2.. ..20.. ..throw..))
    (provided
      (utils/wrap-let 'test-list '(m/list 20) :bar `(throw (IllegalArgumentException. "No match")))
      => `(if ..c2.. ..20.. ..throw..)
      (utils/wrap-let 'test-list '(m/list 10) :foo `(if ..c2.. ..20.. ..throw..))
      => `(if ..c1.. ..10.. (if ..c2.. ..20.. ..throw..))))

  (fact "adds catch-all"
    (utils/match* 'test-list
      '(m/list 10) :foo
      '_ :bar)
    => `(if ..c1.. ..10.. :bar)
    (provided
      (utils/wrap-let 'test-list '(m/list 10) :foo :bar)
      => `(if ..c1.. ..10.. :bar))))
