(ns cloverage.sample.exercise-instrumentation
  (:refer-clojure :exclude [loop])
  (:import java.lang.RuntimeException)
  (:require [clojure.test :refer :all]))

'()

(+ 40)

(+ 40 2)

(str 1 2 3)

(+ (* 2 3)
   (/ 12 3))

(let [a (+ 40 2)
      b (+ 3 4)]
  (* a b))

{:a (+ 40 2)
 (/ 4 2) "two"}

(defn function-with-empty-list []
  ;; used to break stuff - see issues #14 and #17
  '())

(defn not-covered-at-all
  "This function is not covered at all"
  [arg1 arg2]
  (+ 2 3)
  (- 2 3))

(defn partially-covered
  [cnd]
  (if cnd (+ 1 2 3) (- 2 3 4)))

(deftest test-partially-covered
  (is (= 6 (partially-covered true))))

(defn fully-covered [cnd]
  (if cnd (+ 1 2 3) (- 4 5 6)))

(deftest test-fully-covered
  (is (= 6 (fully-covered true)))
  (is (= -7 (fully-covered false))))

(defmulti mixed-coverage-multi type)

(defmethod mixed-coverage-multi String
  ;; fully covered
  [x]
  (do (#()) ; no-op
      x))

(defmethod mixed-coverage-multi Long
  ;; partially covered
  [x]
  (if (= x 1)
    (+ x 2)
    (- x 2)))

(defmethod mixed-coverage-multi Character
  ;; not covered
  [x]
  (str x))

(deftest test-mixed-multi
  (is "String" (mixed-coverage-multi "String"))
  (is 3 (mixed-coverage-multi 1)))

(defmulti fully-covered-multi type)
(defmethod fully-covered-multi String [x] x)
(defmethod fully-covered-multi :default [x] x)
(deftest test-fully-covered-multi
  (is "String" (fully-covered-multi "String"))
  (is 1 (fully-covered-multi 1)))

(defn palindrome?
  "Tests whether s is a palindrom."
  ;; fully covered
  [s]
  (if-not (vector? s)
    (palindrome? (vec s))
    (if (<= (count s) 1)
      true
      (and (= (s 0) (s (dec (count s))))
           (palindrome? (subvec s 1 (dec (count s))))))))

(deftest test-palindrome
  ;; Palindrome is fully covered
  (is (palindrome? "noon"))
  (is (palindrome? "racecar"))
  (is (not (palindrome? "hello"))))

(defn permutation?
  "Tests whether a and b are permutations of each other"
  [a b]
  (and (= (count a)
          (count b))
       (let [add-occurrence (fn [m c] (assoc m c (inc (m c 0))))
             a-counts (reduce add-occurrence {} a)
             b-counts (reduce add-occurrence {} b)]
         (= a-counts b-counts))))

(deftest test-permutation
  ;; permutation is partially covered
  (is (not (permutation? "foo" "foobar"))))

(defn fully-covered-cond
  [n]
  (cond
    (zero? n) :zero
    :else     :nonzero))

(deftest test-fully-covered-cond
  (is (= :zero (fully-covered-cond 0)))
  (is (= :nonzero (fully-covered-cond 1))))

(defn transaction-fn
  [n]
  (dosync
   (cond
     (zero? n) :zero
     :else     (throw (RuntimeException. "FAIL TRANSACTION")))))

(deftest failing-transaction
  (is (thrown? Exception (transaction-fn 1))))

(letfn [(covered [] (+ 2 3))
        (not-covered [] {:and :not-tracked})
        (not-covered [] ({:preimage :image} :preimage))]
  (covered))

(defn loop "Not really loop."
  [n] (+ n n))

(defn global-loop-shouldnt-crash []
  (loop 3))

;; top-level propagate-line-numbers broke preconditions
(defn make-matrix
  ([data ncol]
   {:pre [true]}
   :ok))

(defn locals-dont-crash []
  (let [letfn #(+ % 42)]
    (letfn 2)))

(defn inline-use []
  (bytes (byte-array (map byte [1 2]))))

(deftest CLJ-1330-workaround []
  (is (not= (type (inline-use))
            clojure.lang.Cons)))
