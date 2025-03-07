/*

This file is part of the Fuzion language implementation.

The Fuzion language implementation is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published
by the Free Software Foundation, version 3 of the License.

The Fuzion language implementation is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License along with The
Fuzion language implementation.  If not, see <https://www.gnu.org/licenses/>.

*/

/*-----------------------------------------------------------------------
 *
 * Tokiwa Software GmbH, Germany
 *
 * Source of class AbstractType
 *
 *---------------------------------------------------------------------*/

package dev.flang.ast;

import java.util.Set;

import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.FuzionConstants;
import dev.flang.util.List;
import dev.flang.util.SourcePosition;
import dev.flang.util.YesNo;


/**
 * AbstractType represents a Fuzion Type in the front end.  This type might
 * either be part of the abstract syntax tree or part of a binary module file.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public abstract class AbstractType extends ANY implements Comparable<AbstractType>
{


  /*----------------------------  variables  ----------------------------*/


  /**
   * Cached result of dependsOnGenerics().
   */
  public YesNo _dependsOnGenerics = YesNo.dontKnow;


  /**
   * Cached results for actualType(t) and actualType(f, List<AbstractType>);
   */
  private Object _actualTypeCachedFor1;
  private AbstractType _actualTypeCache;
  private Object _actualType2CachedFor1;
  private Object _actualType2CachedFor2;
  private AbstractType _actualType2Cache;


  /*-----------------------------  methods  -----------------------------*/


  /**
   * setOuter
   *
   * @param t
   */
  void setOuter(Type t)
  {
    throw new Error("AbstractType.setOuter() should only be called on dev.flang.ast.Type");
  }


  /**
   * Find all the types used in this that refer to formal generic arguments of
   * this or any of this' outer classes.
   *
   * This is only needed for ast.Type, for fe.LibraryType
   * this is a NOP.
   *
   * @param feat the root feature that contains this type.
   */
  void findGenerics(AbstractFeature outerfeat)
  {
  }


  /**
   * resolve 'abc.this.type' within a type feature.  This is only needed for
   * ast.Type, for fe.LibraryType this is a NOP.
   *
   * @param feat the outer feature that this type is declared in.
   */
  AbstractType resolveThisType(AbstractFeature outerfeat)
  {
    return this;
  }


  /**
   * resolve this type. This is only needed for ast.Type, for fe.LibraryType
   * this is a NOP.
   *
   * @param res this is called during type resolution, res gives the resolution
   * instance.
   *
   * @param feat the outer feature that this type is declared in, used
   * for resolution of generic parameters etc.
   */
  AbstractType resolve(Resolution res, AbstractFeature outerfeat)
  {
    return this;
  }


  /**
   * For a Type that is not a generic argument, resolve the feature of that
   * type.  Unlike Type.resolve(), this does not check the generic arguments, so
   * this can be used for type inferencing for the actual generics as in a match
   * case.
   *
   * This is only needed for ast.Type, for fe.LibraryType this is a NOP.
   *
   * @param feat the outer feature that this type is declared in, used
   * for resolution of generic parameters etc.
   */
  void resolveFeature(Resolution res, AbstractFeature outerfeat)
  {
  }


  /**
   * This is used only during early phases of the front end before types where
   * checked if they are or contains generics.
   */
  boolean checkedForGeneric()
  {
    return true;
  }


  /**
   * is this a formal generic argument that is open, i.e., the last argument in
   * a formal generic arguments list and followed by ... as A in
   * Function<R,A...>.
   *
   * This type needs very special treatment, it is allowed only as an argument
   * type of the last argument in an abstract feature declaration.  When
   * replacing generics by actual generics arguments, this gets replaced by a
   * (possibly empty) list of actual types.
   *
   * @return true iff this is an open generic
   */
  public boolean isOpenGeneric()
  {
    if (PRECONDITIONS) require
      (checkedForGeneric());

    return isGenericArgument() && genericArgument().isOpen();
  }


  /**
   * Check if this.isOpenGeneric(). If so, create a compile-time error.
   *
   * @return true iff !isOpenGeneric()
   */
  public boolean ensureNotOpen()
  {
    boolean result = true;

    if (PRECONDITIONS) require
      (checkedForGeneric());

    if (isOpenGeneric())
      {
        AstErrors.illegalUseOfOpenFormalGeneric(pos2BeRemoved(), genericArgument());
        result = false;
      }
    return result;
  }


  /**
   * Check if this is a choice type.
   */
  public boolean isChoice()
  {
    return !isGenericArgument() && featureOfType().isChoice();
  }


  /**
   * For a resolved type, check if it is a choice type and if so, return the
   * list of choices. Otherwise, return null.
   */
  public List<AbstractType> choiceGenerics()
  {
    if (PRECONDITIONS) require
      (isGenericArgument() || !(this instanceof Type tt) || tt.feature != null);  // type must be resolved

    if (!isGenericArgument())
      {
        var g = featureOfType().choiceGenerics();
        if (g != null)
          {
            return replaceGenerics(g)
              .map(t -> t.replace_this_type_by_actual_outer(this));
          }
      }
    return null;
  }


  /**
   * Check if this or any of its generic arguments is Types.t_ERROR.
   */
  public boolean containsError()
  {
    return false;
  }


  /**
   * Check if this or any of its generic arguments is Types.t_UNDEFINED.
   *
   * @param exceptFirstGenericArg if true, the first generic argument may be
   * Types.t_UNDEFINED.  This is used in a lambda 'x -> f x' of type
   * 'Function<R,X>' when 'R' is unknown and to be inferred.
   */
  public boolean containsUndefined(boolean exceptFirst)
  {
    return false;
  }


  /**
   * Check if a value of static type actual can be assigned to a field of static
   * type this.  This performs static type checking, i.e., the types may still
   * be or depend on generic parameters.
   *
   * @param actual the actual type.
   */
  public boolean isAssignableFrom(AbstractType actual)
  {
    return isAssignableFrom(actual, null);
  }


  /**
   * Check if a value of static type actual can be assigned to a field of static
   * type this without tagging. This performs static type checking, i.e.,
   * the types may still be or depend on generic parameters.
   *
   * @param actual the actual type.
   */
  public boolean isDirectlyAssignableFrom(AbstractType actual)
  {
    return (!isChoice() && isAssignableFrom(actual))
         || (isChoice() && compareTo(actual) == 0);
  }


  /**
   * Check if a value of static type actual can be assigned to a field of static
   * type this.  This performs static type checking, i.e., the types may still
   * be or depend on generic parameters.
   *
   * In case any of the types involved are or contain t_ERROR, this returns
   * true. This is convenient to avoid the creation of follow-up errors in this
   * case.
   *
   * @param actual the actual type.
   */
  public boolean isAssignableFromOrContainsError(AbstractType actual)
  {
    return
      containsError() || actual.containsError() || isAssignableFrom(actual);
  }


  /**
   * Check if a value of static type actual can be assigned to a field of static
   * type this.  This performs static type checking, i.e., the types may still
   * be or depend on generic parameters.
   *
   * @param actual the actual type.
   *
   * @param assignableTo in case we want to show all types actual is assignable
   * to in an error message, this collects the types converted to strings.
   */
  public boolean isAssignableFrom(AbstractType actual, Set<String> assignableTo)
  {
    if (PRECONDITIONS) require
      (this  .isGenericArgument() || this  .featureOfType() != null || Errors.count() > 0,
       actual.isGenericArgument() || actual.featureOfType() != null || Errors.count() > 0,
       Errors.count() > 0 || this != Types.t_ERROR && actual != Types.t_ERROR);

    if (assignableTo != null)
      {
        assignableTo.add(actual.toString());
      }
    var target_type = this  .remove_type_parameter_used_for_this_type_in_type_feature();
    var actual_type = actual.remove_type_parameter_used_for_this_type_in_type_feature();
    var result =
      target_type.compareTo(actual_type          ) == 0 ||
      actual_type.compareTo(Types.resolved.t_void) == 0 ||
      target_type == Types.t_ERROR                      ||
      actual_type == Types.t_ERROR;
    if (!result && !target_type.isGenericArgument() && isRef() && actual_type.isRef())
      {
        if (actual_type.isGenericArgument())
          {
            result = isAssignableFrom(actual_type.genericArgument().constraint().asRef());
          }
        else
          {
            if (CHECKS) check
              (actual_type.featureOfType() != null || Errors.count() > 0);
            if (actual_type.featureOfType() != null)
              {
                for (var p: actual_type.featureOfType().inherits())
                  {
                    var pt = Types.intern(actual_type
                                          .actualType(p.type())
                                          .replace_this_type_by_actual_outer(actual_type));
                    if (actual_type.isRef())
                      {
                        pt = pt.asRef();
                      }
                    if (isAssignableFrom(pt, assignableTo))
                      {
                        result = true;
                      }
                  }
              }
          }
      }
    if (!result && target_type.isChoice())
      {
        result = target_type.isChoiceMatch(actual_type);
      }
    return result;
  }


  /**
   * Helper for isAssignableFrom: check if this is a choice type and actual is
   * assignable to one of the generic arguments to this choice.
   *
   * @return true iff this is a choice and actual is assignable to one of the
   * generic arguments of this choice.
   */
  private boolean isChoiceMatch(AbstractType actual)
  {
    if (PRECONDITIONS) require
      (!isGenericArgument() && featureOfType() != null || Errors.count() > 0);

    boolean result = false;
    if (!isGenericArgument() && !isRef() && featureOfType().isChoice())
      {
        for (var t : choiceGenerics())
          {
            if (CHECKS) check
              (Errors.count() > 0 || t != null);
            result = result || t != null && Types.intern(t).isAssignableFrom(actual);
          }
      }
    return result;
  }


  /**
   * Check if a type parameter actual can be assigned to a type parameter with
   * constraint this.
   *
   * @param actual the actual type.
   */
  public boolean constraintAssignableFrom(AbstractType actual)
  {
    if (PRECONDITIONS) require
      (Types.intern(this  ) == this,
       Types.intern(actual) == actual,
       this  .isGenericArgument() || this  .featureOfType() != null || Errors.count() > 0,
       actual.isGenericArgument() || actual.featureOfType() != null || Errors.count() > 0,
       Errors.count() > 0 || this != Types.t_ERROR && actual != Types.t_ERROR);

    var result = containsError()                   ||
      actual.containsError()                       ||
      this  .compareTo(actual               ) == 0 ||
      actual.compareTo(Types.resolved.t_void) == 0;
    if (!result && !isGenericArgument())
      {
        if (actual.isGenericArgument())
          {
            result = constraintAssignableFrom(actual.genericArgument().constraint());
          }
        else
          {
            if (CHECKS) check
              (actual.featureOfType() != null || Errors.count() > 0);
            if (actual.featureOfType() != null)
              {
                if (actual.featureOfType() == featureOfType())
                  {
                    if (actual.generics().size() == generics().size()) // NYI: Check: What about open generics?
                      {
                        result = true;
                        // NYI: Should we check if the generics are assignable as well?
                        //
                        //  for (int i = 0; i < _generics.size(); i++)
                        //    {
                        //      var g0 = _generics.get(i);
                        //      var g = _generics.get(i);
                        //      if (g.isGenericArgument())
                        //        {
                        //          g = g.generic.constraint();
                        //        }
                        //      result = result && g0.constraintAssignableFrom(actual._generics.get(i));
                        //    }
                      }
                  }
                if (!result)
                  {
                    for (var p: actual.featureOfType().inherits())
                      {
                        var pt = Types.intern(actual.actualType(p.type()));
                        if (constraintAssignableFrom(pt))
                          {
                            result = true;
                          }
                      }
                  }
              }
          }
      }
    return result;
  }


  /**
   * Replace generic types used in given List of types by the actual generic arguments
   * given as actualGenerics.
   *
   * @param f the feature the generics belong to.
   *
   * @param genericsToReplace a list of possibly generic types
   *
   * @param actualGenerics the actual generics to feat that should replace the
   * formal generics found in genericsToReplace.
   *
   * @return a new list of types with all formal generic arguments from this
   * replaced by the corresponding actualGenerics entry.
   */
  private static List<AbstractType> actualTypes(AbstractFeature f, List<AbstractType> genericsToReplace, List<AbstractType> actualGenerics)
  {
    if (PRECONDITIONS) require
      (Errors.count() > 0 ||
       f.generics().sizeMatches(actualGenerics));

    List<AbstractType> result;
    if (genericsToReplace instanceof FormalGenerics.AsActuals aa && aa.actualsOf(f))  /* shortcut for properly handling open generics list */
      {
        result = actualGenerics;
      }
    else
      {
        result = genericsToReplace.map(t -> t.actualType(f, actualGenerics));
      }
    return result;
  }


  /**
   * Replace formal generics from this type's feature in given list by the
   * actual generic arguments of this type.
   *
   * @param genericsToReplace a list of possibly generic types
   *
   * @return a new list of types with all formal generic arguments from
   * featureOfType() replaced by the corresponding generics entry of this type.
   */
  public List<AbstractType> replaceGenerics(List<AbstractType> genericsToReplace)
  {
    if (PRECONDITIONS) require
      (Errors.count() > 0 ||
       featureOfType().generics().sizeMatches(generics()));

    return actualTypes(featureOfType(), genericsToReplace, generics());
  }


  /**
   * Does this type (or its outer type) depend on generics. If not, actualType()
   * will not need to do anything on this.
   */
  public boolean dependsOnGenerics()
  {

    if (PRECONDITIONS) require
      (checkedForGeneric());

    YesNo result = _dependsOnGenerics;
    if (result == YesNo.dontKnow)
      {
        if (isGenericArgument())
          {
            result = YesNo.yes;
          }
        else
          {
            result = YesNo.no;
            if (generics() != Type.NONE)
              {
                for (var t: generics())
                  {
                    if (CHECKS) check
                      (Errors.count() > 0 || t != null);
                    if (t != null &&
                        t.dependsOnGenerics())
                      {
                        result = YesNo.yes;
                      }
                  }
              }
            if (outer() != null && outer().dependsOnGenerics())
              {
                result = YesNo.yes;
              }
          }
        _dependsOnGenerics = result;
      }
    return result == YesNo.yes;
  }


  /**
   * Replace generic types used in given type t by the actual generic arguments
   * given in this.
   *
   * @param t a possibly generic type, must not be an open generic.
   *
   * @return t with all generic arguments from this.featureOfType._generics
   * replaced by this._generics.
   */
  public AbstractType actualType(AbstractType t)
  {
    if (PRECONDITIONS) require
      (checkedForGeneric(),
       t != null,
       t.checkedForGeneric(),
       Errors.count() > 0 || !t.isOpenGeneric(),
       isGenericArgument() || featureOfType().generics().sizeMatches(generics()));

    AbstractType result;
    if (t._actualTypeCachedFor1 == this)
      {
        result = t._actualTypeCache;
      }
    else
      {
        result = actualType_(t);
        t._actualTypeCachedFor1 = this;
        t._actualTypeCache = result;
      }

    if (POSTCONDITIONS) ensure
      (result != null);
    return result;
  }


  /**
   * Replace generic types used in given type t by the actual generic arguments
   * given in this.
   *
   * Internal version of actualType(t) that does not perform caching.
   *
   * @param t a possibly generic type, must not be an open generic.
   *
   * @return t with all generic arguments from this.featureOfType._generics
   * replaced by this._generics.
   */
  private AbstractType actualType_(AbstractType t)
  {
    /* NYI: Performance: This requires time in O(this.depth *
     * featureOfType.inheritanceDepth * t.depth), i.e. it is in O(n³)! Caching
     * is used to alleviate this a bit, but this is probably not sufficient!
     */
    var result = t;
    if (result.dependsOnGenerics())
      {
        if (isGenericArgument())
          {
            result = genericArgument().constraint().actualType(t);
          }
        else
          {
            result = result.actualType(featureOfType(), generics());
            if (outer() != null)
              {
                result = outer().actualType(result);
              }
          }
      }
    return result;
  }


  /**
   * Check if type t depends on a formal generic parameter of this. If so,
   * replace t by the corresponding actual generic parameter from the list
   * provided.
   *
   * @param f the feature actualGenerics belong to.
   *
   * @param actualGenerics the actual generic parameters
   *
   * @return t iff t does not depend on a formal generic parameter of this,
   * otherwise the type that results by replacing all formal generic parameters
   * of this in t by the corresponding type from actualGenerics.
   */
  public AbstractType actualType(AbstractFeature f, List<AbstractType> actualGenerics)
  {
    if (PRECONDITIONS) require
      (checkedForGeneric(),
       Errors.count() > 0 ||
       f.generics().sizeMatches(actualGenerics),
       Errors.count() > 0 || !isOpenGeneric() || genericArgument().formalGenerics() != f.generics());

    AbstractType result;
    if (_actualType2CachedFor1 == f &&
        _actualType2CachedFor2 == actualGenerics)
      {
        result = _actualType2Cache;
      }
    else
      {
        result = actualType_(f, actualGenerics);
        _actualType2CachedFor1 = f;
        _actualType2CachedFor2 = actualGenerics;
        actualGenerics.freeze();
        _actualType2Cache = result;
      }
    return result;
  }


  /**
   * Is this the type of a type feature, e.g., the type of `(list
   * i32).type`. Will return false for an instance of Type for which this is
   * still unknown since Type.resolve() was not called yet.
   */
  boolean isTypeType()
  {
    return !isGenericArgument() && featureOfType().isTypeFeature();
  }


  /**
   * Check if this type depends on a formal generic parameter of f. If so,
   * replace t by the corresponding actual generic parameter from the list
   * provided.
   *
   * Internal version of actualType(t) that does not perform caching.
   *
   * @param f the feature actualGenerics belong to.
   *
   * @param actualGenerics the actual generic parameters
   *
   * @return t iff t does not depend on a formal generic parameter of this,
   * otherwise the type that results by replacing all formal generic parameters
   * of this in t by the corresponding type from actualGenerics.
   */
  private AbstractType actualType_(AbstractFeature f, List<AbstractType> actualGenerics)
  {
    /* NYI: Performance: This requires time in O(this.depth *
     * f.inheritanceDepth), i.e. it is in O(n²)!  Caching is used to alleviate
     * this a bit, but this is probably not sufficient!
     */
    var result = this;
    if (f != null)
      {
        for (var i : f.inherits())
          {
            result = result.actualType(i.calledFeature(),
                                       i.actualTypeParameters());
          }
      }
    if (result.isGenericArgument())
      {
        Generic g = result.genericArgument();
        if (f != null && g.formalGenerics() == f.generics())  // t is replaced by corresponding actualGenerics entry
          {
            result = result.ensureNotOpen() ? g.replace(actualGenerics)
                                            : Types.t_ERROR;
          }
      }
    else
      {
        var g2 = actualTypes(f, result.generics(), actualGenerics);
        var o2 = (result.outer() == null) ? null : result.outer().actualType(f, actualGenerics);

        if (isTypeType())
          {
            var this_type = g2.get(0);
            g2 = g2.map(x -> x == this_type ? x   // leave first type parameter unchanged
                                            : x.actualTypeType(this_type));
          }

        if (g2 != result.generics() ||
            o2 != result.outer()       )
          {
            var hasError = o2 == Types.t_ERROR;
            for (var t : g2)
              {
                hasError = hasError || (t == Types.t_ERROR);
              }
            result = hasError ? Types.t_ERROR : result.actualType(g2, o2);
          }
      }
    return result;
  }



  /**
   * Helper for actualType_ to determine the actual type of a type feature's
   * type. This needs special handling since the the type feature has one
   * additional first type parameter --the underlying type: this_type--, and all
   * other type parameters need converted to the actual type relative to that.
   *
   * @param this_type the first type parameter that contains the actual type.
   */
  private AbstractType actualTypeType(AbstractType this_type)
  {
    var t = replace_this_type_by_actual_outer(this_type);
    if (!this_type.isGenericArgument() && !this_type.featureOfType().isUniverse())
      {
        t = t.actualTypeType(this_type.outer());
        t = t.actualType(this_type.featureOfType(), this_type.generics());
      }
    return t;
  }


  /**
   * For a type that is not a type parameter, create a new variant using given
   * actual generics and outer type.
   *
   * @param g2 the new actual generics to be used
   *
   * @param o2 the new outer type to be used (which may also differ in its
   * actual generics).
   *
   * @return a new type with same featureOfType(), but using g2/o2 as generics
   * and outer type.
   */
  public AbstractType actualType(List<AbstractType> g2, AbstractType o2)
  {
    if (PRECONDITIONS) require
      (!isGenericArgument());

    throw new Error("actualType not supported for "+getClass());
  }


  /**
   * Check that in case this is a choice type, it is valid, i.e., it is a value
   * type and the generic arguments to the choice are different.  Create compile
   * time error in case this is not the case.
   */
  void checkChoice(SourcePosition pos)
  {
    var g = choiceGenerics();
    if (g != null)
      {
        if (isRef())
          {
            AstErrors.refToChoice(pos);
          }

        int i1 = 0;
        for (var t1 : g)
          {
            t1 = Types.intern(t1);
            int i2 = 0;
            for (var t2 : g)
              {
                t2 = Types.intern(t2);
                if (i1 < i2)
                  {
                    if ((t1 == t2 ||
                         !t1.isGenericArgument() &&
                         !t2.isGenericArgument() &&
                         (t1.isDirectlyAssignableFrom(t2) ||
                          t2.isDirectlyAssignableFrom(t1) )) &&
                        t1 != Types.t_ERROR &&
                        t2 != Types.t_ERROR)
                      {
                        AstErrors.genericsMustBeDisjoint(pos, t1, t2);
                      }
                  }
                i2++;
              }
            i1++;
          }
      }
  }

  public AbstractType visit(FeatureVisitor v, AbstractFeature outerfeat)
  {
    throw new Error("AbstractType.visit not implemented by "+getClass());
  }


  /**
   * isFunType checks if this is a function type, e.g., "fun (int x,y) String".
   *
   * @return true iff this is a fun type
   */
  public boolean isFunType()
  {
    return
      !isGenericArgument() &&
      (featureOfType() == Types.resolved.f_function ||
       featureOfType() == Types.resolved.f_Unary);
  }


  /**
   * isLazyType checks if this is a lazy function type.
   *
   * @return true iff this is a lazy type
   */
  public boolean isLazyType()
  {
    return
      !isGenericArgument() &&
      featureOfType() == Types.resolved.f_Lazy;
  }


  /**
   * Find a type that is assignable from values of two types, this and t. If no
   * such type exists, return Types.t_UNDEFINED.
   *
   * @param that another type or null
   *
   * @return a type that is assignable both from this and that, or Types.t_UNDEFINED if none
   * exists.
   */
  AbstractType union(AbstractType that)
  {
    AbstractType result =
      this == Types.t_ERROR                      ? Types.t_ERROR     :
      that == Types.t_ERROR                      ? Types.t_ERROR     :
      this == Types.t_UNDEFINED                  ? Types.t_UNDEFINED :
      that == Types.t_UNDEFINED                  ? Types.t_UNDEFINED :
      this.compareTo(Types.resolved.t_void) == 0 ? that              :
      that.compareTo(Types.resolved.t_void) == 0 ? this              :
      this.isAssignableFrom(that)                ? this :
      that.isAssignableFrom(this)                ? that :
      this.isAssignableFrom(that.asRef())        ? this :
      that.isAssignableFrom(this.asRef())        ? that : Types.t_UNDEFINED;

    if (POSTCONDITIONS) ensure
      (result == Types.t_UNDEFINED ||
       result == Types.t_ERROR     ||
       this.compareTo(Types.resolved.t_void) == 0 && result == that ||
       that.compareTo(Types.resolved.t_void) == 0 && result == this ||
       (result.isAssignableFrom(this) || result.isAssignableFrom(this.asRef()) &&
        result.isAssignableFrom(that) || result.isAssignableFrom(that.asRef())    ));

    return result;
  }


  /**
   * Compare this to other for creating unique types.
   */
  public int compareTo(AbstractType other)
  {
    if (PRECONDITIONS) require
      (checkedForGeneric(),
       other != null,
       other.checkedForGeneric());

    int result = compareToIgnoreOuter(other);
    if (result == 0 && !isGenericArgument())
      {
        var to = this .outer();
        var oo = other.outer();
        result =
          (to == null && oo == null) ?  0 :
          (to == null && oo != null) ? -1 :
          (to != null && oo == null) ? +1 : Types.intern(to).compareTo(Types.intern(oo));
      }
    return result;
  }


  /**
   * Compare this to other ignoring the outer type. This is used for created in
   * clazzes when the outer clazz is known.
   */
  public int compareToIgnoreOuter(AbstractType other)
  {
    if (PRECONDITIONS) require
      (checkedForGeneric(),
       other != null,
       other.checkedForGeneric());

    int result = 0;

    if (this != other)
      {
        result =
          isGenericArgument() &&  other.isGenericArgument() ?  0 :
          isGenericArgument() && !other.isGenericArgument() ? -1 :
          !isGenericArgument() && other.isGenericArgument() ? +1 : featureOfType().compareTo(other.featureOfType());
        if (!isGenericArgument())
          {
            if (result == 0)
              {
                if (generics().size() != other.generics().size())  // this may happen for open generics lists
                  {
                    result = generics().size() < other.generics().size() ? -1 : +1;
                  }
                else
                  {
                    var tg = generics().iterator();
                    var og = other.generics().iterator();
                    while (tg.hasNext() && result == 0)
                      {
                        var tgt = tg.next();
                        var ogt = og.next();

                        if (CHECKS) check
                          (Errors.count() > 0 || tgt != null && ogt != null);

                        if (tgt != null && ogt != null)
                          {
                            result = tgt.compareTo(ogt);
                          }
                      }
                  }
              }
          }
        if (result == 0)
          {
            result = name().compareTo(other.name());
          }
        if (result == 0)
          {
            if (isRef() ^ other.isRef())
              {
                result = isRef() ? -1 : 1;
              }
          }
        if (result == 0)
          {
            if (isThisType() ^ other.isThisType())
              {
                result = isThisType() ? -1 : 1;
              }
          }
        if (isGenericArgument())
          {
            if (result == 0)
              {
                result = genericArgument().feature().compareTo(other.genericArgument().feature());
                if (result == 0)
                  {
                    result = genericArgument().name().compareTo(other.genericArgument().name()); // NYI: compare generic, not generic.name!
                  }
              }
          }
      }
    return result;
  }


  public String name()
  {
    return isGenericArgument() ? genericArgument().name() : featureOfType().featureName().baseName();
  }


  /**
   * This must be called on a call result type to replace `this.type` used in
   * the result type by the actual type dictated by the target of the call
   *
   * example:
   *
   *   a is
   *
   *     l list a.this.type is [a.this].as_list
   *
   *   b : a is
   *
   *   say (type_of a.l)    # should print `list a`
   *   say (type_of b.l)    # should print `list b`
   *
   * @param tt the type feature we are calling (`equatable.type` in the example)
   * above).
   */
  public AbstractType replace_this_type_by_actual_outer(AbstractType tt)
  {
    var result = this;
    do
      {
        result = result.replace_this_type_by_actual_outer2(tt);
        tt = tt.isGenericArgument() ? null : tt.outer();
      }
    while (tt != null);
    return result;
  }


  /**
   * Helper for replace_this_type_by_actual_outer to replace `this.type` for
   * exactly tt, ignoring tt.outer().
   *
   * @param tt the type feature we are calling
   */
  private AbstractType replace_this_type_by_actual_outer2(AbstractType tt)
  {
    var result = this;
    if (isThisType())
      {
        var att = (tt.isGenericArgument() ? tt.genericArgument().constraint() : tt);
        if (att.featureOfType().inheritsFrom(featureOfType()))
          {
            result = tt;
          }
      }
    else
      {
        result = applyToGenericsAndOuter(g -> g.replace_this_type_by_actual_outer2(tt));
      }
    return result;
  }


  /**
   * For a given type t, get the type of t's type feature. E.g., for t==string,
   * this will return the type of string.type.
   *
   * @param t the type whose type's type we want to get
   *
   * @return the type of t's type.
   */
  public AbstractType typeType()
  {
    if (PRECONDITIONS) require
      (!isGenericArgument(),
       featureOfType().state().atLeast(Feature.State.RESOLVED));

    return typeType(null);
  }


  /**
   * For a given type t, get the type of t's type feature. E.g., for t==string,
   * this will return the type of string.type, which is 'string.#type string'
   *
   * @param res Resolution instance used to resolve the type feature that might
   * need to be created.
   *
   * @param t the type whose type's type we want to get
   *
   * @return the type of t's type.
   */
  AbstractType typeType(Resolution res)
  {
    if (PRECONDITIONS) require
      (!isGenericArgument(),
       res != null || featureOfType().state().atLeast(Feature.State.RESOLVED));

    var result = this;
    var fot = featureOfType();
    if (!fot.isUniverse() && this != Types.t_ERROR)
      {
        var f =
          fot.isTypeFeature()  ? null                 :
          fot.hasTypeFeature() ? fot.typeFeature()    :
          res != null          ? fot.typeFeature(res)
                               : null;
        if (f == null)  // NYI: This is the case for fot.isTypeFeature(), but also for some internal features like #anonymous. Need to check why.
          {
            result = Types.resolved.f_Type.selfType();
          }
        else
          {
            var g = new List<AbstractType>(this);
            g.addAll(generics());
            result = Types.intern(new Type(f.pos(),
                                           f.featureName().baseName(),
                                           g,
                                           outer().typeType(res),
                                           f,
                                           Type.RefOrVal.Value));
          }
      }
    return result;
  }


  /**
   * This should be called on a formal argument in call with given `target`.  If
   * `target` is a type parameter and the formal argument type `this` depends on
   * a type features `this.type`, then replace `this.type` by the type parameter
   * `target`.
   *
   * example:
   *
   *   equatable is
   *
   *     type.equality(a, b equatable.this.type) bool is abstract
   *
   *   equals(T type : equatable, x, y T) => T.equality x y
   *
   * For the call `T.equality x y` this will be called on the formal argument
   * type for `a` (and `b`).
   *
   * The type of the formal arguments `a` and `b` is `equatable.this.type`,
   * which was replaced by the implicit first generic argument of
   * `equatable.type`.  This method will replaced it by `T` in the call
   * `T.equality x y`, such that actual arguments of the same type are
   * assignment compatible to it.
   *
   * @param tf the type feature we are calling (`equatable.type` in the example
   * above).
   *
   * @param tc the target call (`T` in the example above).
   */
  AbstractType replace_type_parameter_used_for_this_type_in_type_feature(AbstractFeature tf, AbstractCall tc)
  {
    var result = this;
    if (isGenericArgument())
      {
        if (genericArgument().typeParameter() == tf.arguments().get(0))
          { // a call of the form `T.f x` where `f` is declared as
            // `abc.type.f(arg abc.this.type)`, so replace
            // `abc.this.type` by `T`.
            result = new Type(tc.pos(), new Generic(tc.calledFeature()));
          }
      }
    else
      {
        result = applyToGenericsAndOuter(g -> g.replace_type_parameter_used_for_this_type_in_type_feature(tf, tc));
      }
    return result;
  }


  /**
   * Replace implicit generic type used for `abc.this.type` in `abc.type` by
   * `abc.this.type`.
   *
   * example:
   *
   *   num is
   *
   *     type.zero num.this.type is abstract
   *
   *     plus (other num.this.type) num.this.type is abstract
   *
   *     type.sum is
   *       infix ∙ (a, b num.this.type) num.this.type is a.plus b
   *
   * here, when passing `b` to `plus`, `b` is of type `num.this.type`, which was
   * replaced by the implicit first generic argument of `num.type`, but it needs
   * to be changed back to `num.this.type`.
   */
  AbstractType remove_type_parameter_used_for_this_type_in_type_feature()
  {
    var result = this;
    if (isGenericArgument())
      {
        var tp = genericArgument().typeParameter();
        var tf = tp.outer();
        if (tf.isTypeFeature() && tp == tf.arguments().get(0))
          { // generic used for `abc.this.type` in `abc.type` by `abc.this.type`.
            var tfo = tf.typeFeatureOrigin();
            if (tfo != null)  // NYI: tfo may be null for type feature loaded
                              // from .fum file, need to support
                              // typeFeatureOrigin for fe.LibraryFeature!
              {
                result = tfo.selfType().asThis();
              }
          }
      }
    else
      {
        result = applyToGenericsAndOuter(g -> g.remove_type_parameter_used_for_this_type_in_type_feature());
      }
    return result;
  }


  /**
   * For any type parameter g used in this, in cases these are type parameters
   * of the origin of a type feature o, where o is f or an outer feature of f,
   * replace g by the corresponding type parameter of o.
   *
   * This is used to infer type parameter for a call to a feature declared in a
   * type feature where the actual arguments are instances of the original
   * (non-type) feature.
   *
   * @param f the outer feature this type is used in.
   */
  public AbstractType replace_type_parameters_of_type_feature_origin(AbstractFeature f)
  {
    var t = this;
    if (!f.isUniverse() && f != Types.f_ERROR)
      {
        t = t.replace_type_parameters_of_type_feature_origin(f.outer());
        if (f.isTypeFeature())
          {
            t = t.replace_type_parameter_of_type_origin(f);
          }
      }
    return Types.intern(t);
  }


  /**
   * Helper for replace_type_parameters_of_type_feature_origin working on a
   * given outer type feature.
   *
   * @param outerTypeFeature one outer type feature this is used in.
   */
  private AbstractType replace_type_parameter_of_type_origin(AbstractFeature outerTypeFeature)
  {
    if (PRECONDITIONS) require
      (outerTypeFeature.isTypeFeature());

    AbstractType result;
    if (isGenericArgument())
      {
        if (genericArgument().feature() == outerTypeFeature.typeFeatureOrigin())
          {
            result = new Type(this.pos2BeRemoved(), outerTypeFeature.generics().list.get(genericArgument().index() + 1));
          }
        else
          {
            result = this;
          }
      }
    else
      {
        result = applyToGenericsAndOuter(g -> g.replace_type_parameter_of_type_origin(outerTypeFeature));
      }
    return result;
  }


  /**
   * Apply given function recursively to generics and outer types in this type
   * to create a new type.
   *
   * @param f function to apply to generics and outer types
   *
   * @return in case f resulted in any changes, a new type with generics and
   * outer types replaced by the corresponding results of f.apply.  this in case
   * the were no changes.
   */
  public AbstractType applyToGenericsAndOuter(java.util.function.Function<AbstractType, AbstractType> f)
  {
    var result = this;
    if (!isGenericArgument())
      {
        var g = generics();
        var ng = g.map(f);
        var o = outer();
        var no = o != null ? f.apply(o) : null;
        if (ng != g || no != o)
          {
            result = new Type(this, ng, no);
          }
      }
    return result;
  }


  /**
   * Check if for this or any type parameters of this, isThisType is true.  This
   * must not be the case for any clazzes in FUIR since clazzes require concrete
   * types.
   *
   * @return true if an `this.type` where found
   */
  public boolean containsThisType()
  {
    return
      isThisType() ||
      !isGenericArgument() && generics().stream().anyMatch(g -> g.containsThisType()) ||
      outer() != null && outer().containsThisType();
  }


  public abstract AbstractFeature featureOfType();
  public abstract AbstractType asRef();
  public abstract AbstractType asValue();
  public abstract boolean isRef();
  public abstract AbstractType asThis();
  public abstract boolean isThisType();
  public abstract SourcePosition pos2BeRemoved();
  public abstract List<AbstractType> generics();
  public abstract boolean isGenericArgument();
  public abstract AbstractType outer();
  public abstract Generic genericArgument();


  /**
   * Get a String representation of this Type.
   *
   * Note that this does not work for instances of Type before they were
   * resolved.  Use toString() for creating strings early in the front end
   * phase.
   */
  public String asString()
  {
    if (PRECONDITIONS) require
      (checkedForGeneric());

    String result;

    if (isGenericArgument())
      {
        var ga = genericArgument();
        result = ga.feature().qualifiedName() + "." + ga.name() + (this.isRef() ? " (boxed)" : "");
      }
    else
      {
        var o = outer();
        String outer = o != null && !o.featureOfType().isUniverse() ? o.asStringWrapped() + "." : "";
        var f = featureOfType();
        var fn = f.featureName();
        // for a feature that does not define a type itself, the name is not
        // unique due to overloading with different argument counts. So we add
        // the argument count to get a unique name.
        var fname = fn.baseName() + (f.definesType() ? "" : FuzionConstants.INTERNAL_NAME_PREFIX + fn.argCount());
        result = outer
              + (isRef() != featureOfType().isThisRef() ? (isRef() ? "ref " : "value ") : "" )
              + fname;
        if (isThisType())
          {
            result = result + ".this.type";
          }
        for (var g : generics())
          {
            result = result + " " + g.asStringWrapped();
          }
      }
    return result;
  }


  /**
   * toString
   *
   * @return
   */
  public String toString()
  {
    return asString();
  }


  /**
   * wrap the result of toString in parentheses if necessary
   */
  public String toStringWrapped()
  {
    var s = toString();
    return s.contains(" ")
           ? "(" + s + ")"
           : s;
  }


  /**
   * wrap the result of asString in parentheses if necessary
   */
  public String asStringWrapped()
  {
    var s = asString();
    return s.contains(" ") ? "(" + s + ")"
                           :       s      ;
  }


  /**
   * Check if constraints of this type are satisfied.
   * Returns itself on success or t_ERROR if constraints are not met.
   */
  // NYI Can this result in an infinite recursion?
  public AbstractType checkConstraints(SourcePosition pos)
  {
    // NYI caching?
    var result = this;
    if (!isGenericArgument())
      {
        // NYI deduplicate this code?: also in Call.checkTypes()

        // Check that generics match formal generic constraints
        var fi = featureOfType().generics().list.iterator();
        var gi = generics().iterator();
        while (fi.hasNext() &&
              gi.hasNext()    ) // NYI: handling of open generic arguments
          {
            var f = fi.next();
            var g = gi.next();
            g.checkConstraints(pos);
            if (compareTo(f.constraint()) != 0)
              {
                f.constraint().checkConstraints(pos);
              }

            if (CHECKS) check
              (Errors.count() > 0 || f != null && g != null);
            if (f != null && g != null &&
                !Types.intern(f.constraint()).constraintAssignableFrom(Types.intern(g)))
              {
                AstErrors.incompatibleActualGeneric(pos, f, g);
                result = Types.t_ERROR;
              }
          }
      }
    return result;
  }


}

/* end of file */
