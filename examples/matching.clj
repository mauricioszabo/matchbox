(require '[matcher.matchers :as m])

(def some-list '(1 2 40 50))

; Feel free to vary any parameter below...
(println (m/match some-list
           (m/list odd? even? odd? even?) "Alternate numbers"
           (m/list odd? odd? odd? odd?) "All odd"
           (m/list even? even? even? even?) "All Even"
           (m/list _ _ even? even?) "Last two are even"
           _ "Didn't find a match"))

(println (m/match some-list
           (m/list ?f ?s odd? even?) (str "Alternate numbers, sum of first two: " (+ f s))
           (m/list odd? odd? & ?rest) (str "First two are odd, rest of the list: " (vec rest))
           (m/vector ?f ?s even? even?) (str "Vector, last two are even, sum of first two: " (+ f s))
           (m/list ?f ?s even? even?) (str "List, last two are even, sum of first two: " (+ f s))
           _ "Didn't find a match"))

; String matchers
(m/match "foo@bar.com"
  (m/regexp #"(.*)@(.*)" ?user ?domain) (println user "AT" domain))

(m/match "foo@bar.com"
  (m/regexp #"(.*)@(.*)" ?user) (println user)
  (m/regexp #"(.*)@(.*)") (println "MATCHES!"))

; Predicate matchers
(println (m/match 20
           odd? "It's odd"
           even? "It's even"))

(println (m/match "20"
           (m/satisfies odd?) "It's odd"
           (m/satisfies even?) "It's even"
           _ "It's not a number"))

; PLEASE note that you probably want to extract the matcher below to a function.
; Matchers can be very sensitive about inner functions and such, and we don't expect
; to use "filter", "some", and other complex operations inside it.
(println (m/match some-list
           (m/satisfies #(some #{-1 -2} %) ?r) (str "Negative number found: " r)
           (m/satisfies #(some #{3 4 50 40} %) ?r) (str "Positive number found: " r)))

; A pratical example
(def some-ast
  '(+ (* 10 2) (* 20 (+ 30 20))))
(defn calculate-result [ast]
  (m/match ast
    (m/list '+ & ?rest) (reduce #(+ %1 (calculate-result %2)) 0 rest)
    (m/list '* & ?rest) (reduce #(+ %1 (calculate-result %2)) 0 rest)
    number? ast))
(println (calculate-result some-ast))

; Nested matchers
(println (m/match some-ast
           (m/list _ list? (m/list _ ?number (m/list _ ?number ?other)))
           (str "Will not match - number must be the same in both places: " (+ number other))

           (m/list _ list? (m/list _ ?number (m/list _ ?other ?number)))
           (str "Will match: " (+ number other))))

; Boolean matches
(def some-vector [3 2 3 4])
(println (m/match some-vector
           (m/or (m/vector ?a ?b) (m/list _ _ ?a ?b)) "Will not match"
           (m/or (m/vector ?a ?b) (m/vector _ _ ?a ?b)) (+ a b)))

(println (m/match some-vector
           (m/and (m/vector ?a _ _ ?b) (m/vector _ ?a _ ?b)) "Will not match - ?a cannot be 3 and 2"
           (m/and (m/vector ?a _ _ ?b) (m/vector _ _ ?a ?b)) (+ a b)))

; Records (Binary tree example)
(defrecord Tree [left value right])
(defn insert [tree new-value]
  (m/match tree
    nil (->Tree nil new-value nil)
    (m/record Tree ?left ?value ?right) (if (< new-value value)
                                          (->Tree (insert left new-value) value right)
                                          (->Tree left value (insert right new-value)))))

(defn tree->list [tree]
  (m/match tree
    nil nil
    (m/record Tree ?left ?value ?right) (concat (tree->list left) (cons value (tree->list right)))))

(-> nil
    (insert 10)
    (insert 25)
    (insert 1)
    (insert 4)
    (insert 40)
    (insert 12)
    (insert 21)
    tree->list)
