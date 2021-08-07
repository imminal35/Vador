package org.revcloud.vader.runner

import io.vavr.Tuple
import io.vavr.Tuple2
import io.vavr.control.Either
import io.vavr.kotlin.left
import io.vavr.kotlin.right
import org.revcloud.vader.types.failures.FFABatchOfBatchFailureWithPair
import org.revcloud.vader.types.failures.FFEBatchOfBatchFailure
import java.util.Optional

internal typealias FailFast<ValidatableT, FailureT> = (ValidatableT) -> Optional<FailureT>

internal typealias FailFastForEach<ValidatableT, FailureT> = (Collection<ValidatableT?>) -> List<Either<FailureT?, ValidatableT?>>

internal typealias FailFastForEachNestedBatch1<ValidatableT, FailureT> = (Collection<ValidatableT?>) -> List<Either<FFEBatchOfBatchFailure<FailureT?>, ValidatableT?>>

internal typealias FailFastForAny<ValidatableT, FailureT> = (Collection<ValidatableT?>) -> Optional<FailureT>

internal typealias FailFastForAnyWithPair<ValidatableT, FailureT, PairT> = (Collection<ValidatableT?>) -> Optional<Tuple2<PairT?, FailureT?>>

internal typealias FailFastForAnyNestedWithPair<ValidatableT, FailureT, ContainerPairT, MemberPairT> = (Collection<ValidatableT?>) -> Optional<FFABatchOfBatchFailureWithPair<ContainerPairT?, MemberPairT?, FailureT?>>

internal typealias FailFastForContainer<ValidatableT, FailureT> = (ValidatableT) -> Optional<FailureT>

@JvmSynthetic
internal fun <ValidatableT, FailureT> failFast(
  validationConfig: ValidationConfig<ValidatableT, FailureT?>,
  throwableMapper: (Throwable) -> FailureT?
): FailFast<ValidatableT, FailureT> = { validatable: ValidatableT ->
  findFirstFailure(right(validatable), toValidators(validationConfig), throwableMapper)
    .toFailureOptional()
}

/**
 * Batch + Simple + Config
 *
 * @param failureForNullValidatable
 * @param throwableMapper
 * @param batchValidationConfig
 * @param <FailureT>
 * @param <ValidatableT>
 * @return
 */
@JvmSynthetic
internal fun <FailureT, ValidatableT> failFastForEach(
  batchValidationConfig: BaseBatchValidationConfig<ValidatableT, FailureT?>,
  failureForNullValidatable: FailureT?,
  throwableMapper: (Throwable) -> FailureT?
): FailFastForEach<ValidatableT, FailureT> = { validatables: Collection<ValidatableT?> ->
  findAndFilterInvalids(
    validatables,
    failureForNullValidatable,
    batchValidationConfig.findAndFilterDuplicatesConfigs
  ).map { findFirstFailure(it, toValidators(batchValidationConfig), throwableMapper) ?: it }
}

@JvmSynthetic
internal fun <ContainerValidatableT, MemberValidatableT, FailureT> failFastForEachNested(
  batchOfBatch1ValidationConfig: BatchOfBatch1ValidationConfig<ContainerValidatableT, MemberValidatableT, FailureT?>,
  failureForNullValidatable: FailureT?,
  throwableMapper: (Throwable) -> FailureT?
): FailFastForEachNestedBatch1<ContainerValidatableT, FailureT> =
  { containerValidatables: Collection<ContainerValidatableT?> ->
    failFastForEach(
      batchOfBatch1ValidationConfig,
      failureForNullValidatable, throwableMapper
    )(
      containerValidatables
    ).map { validContainer: Either<FailureT?, ContainerValidatableT?> ->
      validContainer.map(batchOfBatch1ValidationConfig.withMemberBatchValidationConfig._1)
        .map { members: Collection<MemberValidatableT> ->
          failFastForEach(
            batchOfBatch1ValidationConfig.withMemberBatchValidationConfig._2,
            failureForNullValidatable,
            throwableMapper
          )(members)
        }
        .map { memberResults: List<Either<FailureT?, MemberValidatableT?>> -> memberResults.map { it.flatMap { validContainer } } }
    }.map { result: Either<FailureT?, List<Either<FailureT?, ContainerValidatableT?>>> ->
      result.fold<Either<Either<FailureT?, List<FailureT?>>, ContainerValidatableT?>?>(
        { containerFailure -> left(left(containerFailure)) },
        { memberResults ->
          val memberFailures = memberResults.filter { it.isLeft }
          if (memberFailures.isEmpty()) right(memberFailures.firstOrNull()?.get())
          else left(right(memberFailures.map { it.left }))
        }
      ).mapLeft { FFEBatchOfBatchFailure(it) }
    }
  }

@JvmSynthetic
internal fun <ContainerValidatableT, MemberValidatableT, FailureT> failFastForAnyNested(
  batchOfBatch1ValidationConfig: BatchOfBatch1ValidationConfig<ContainerValidatableT, MemberValidatableT, FailureT?>,
  failureForNullValidatable: FailureT?,
  throwableMapper: (Throwable) -> FailureT?
): FailFastForAny<ContainerValidatableT, FailureT> =
  { containerValidatables: Collection<ContainerValidatableT?> ->
    failFastForAnyNested<ContainerValidatableT, MemberValidatableT, FailureT, Nothing, Nothing>(
      batchOfBatch1ValidationConfig,
      failureForNullValidatable,
      throwableMapper
    )(containerValidatables)
      .map { it.containerFailure ?: it.batchMemberFailure }.flatMap { Optional.ofNullable(it?._2) }
  }

@JvmSynthetic
internal fun <ContainerValidatableT, MemberValidatableT, FailureT, ContainerPairT, MemberPairT> failFastForAnyNested(
  batchOfBatch1ValidationConfig: BatchOfBatch1ValidationConfig<ContainerValidatableT, MemberValidatableT, FailureT?>,
  failureForNullValidatable: FailureT?,
  throwableMapper: (Throwable) -> FailureT?,
  containerPairForInvalidMapper: (ContainerValidatableT?) -> ContainerPairT? = { null },
  memberPairForInvalidMapper: (MemberValidatableT?) -> MemberPairT? = { null }
): FailFastForAnyNestedWithPair<ContainerValidatableT, FailureT, ContainerPairT, MemberPairT> =
  { containerValidatables: Collection<ContainerValidatableT?> ->
    failFastForAny(
      batchOfBatch1ValidationConfig,
      failureForNullValidatable,
      throwableMapper,
      containerPairForInvalidMapper
    )(containerValidatables)
      .map { FFABatchOfBatchFailureWithPair<ContainerPairT?, MemberPairT?, FailureT?>(left(it)) }
      .or {
        containerValidatables.asSequence()
          .map { batchOfBatch1ValidationConfig.withMemberBatchValidationConfig._1.apply(it) }
          .map { members: Collection<MemberValidatableT> ->
            failFastForAny(
              batchOfBatch1ValidationConfig.withMemberBatchValidationConfig._2,
              failureForNullValidatable,
              throwableMapper,
              memberPairForInvalidMapper
            )(members).map {
              FFABatchOfBatchFailureWithPair<ContainerPairT?, MemberPairT?, FailureT?>(
                right(it)
              )
            }
          }
          .firstOrNull { it.isPresent } ?: Optional.empty()
      }
  }

// TODO 13/05/21 gopala.akshintala: Reconsider any advantage of having this as a HOF 
@JvmSynthetic
internal fun <ValidatableT, FailureT> failFastForAny(
  batchValidationConfig: BaseBatchValidationConfig<ValidatableT, FailureT?>,
  nullValidatable: FailureT?,
  throwableMapper: (Throwable) -> FailureT?
): FailFastForAny<ValidatableT, FailureT> = { validatables ->
  failFastForAny<ValidatableT, FailureT, Nothing>(
    batchValidationConfig,
    nullValidatable,
    throwableMapper
  )(validatables).map { it._2 }
}

@JvmSynthetic
internal fun <ValidatableT, FailureT, PairT> failFastForAny(
  batchValidationConfig: BaseBatchValidationConfig<ValidatableT, FailureT?>,
  failureForNullValidatable: FailureT?,
  throwableMapper: (Throwable) -> FailureT?,
  pairForInvalidMapper: (ValidatableT?) -> PairT? = { null }
): FailFastForAnyWithPair<ValidatableT, FailureT, PairT> = { validatables ->
  findFirstInvalid(
    validatables,
    batchValidationConfig.findAndFilterDuplicatesConfigs,
    failureForNullValidatable,
    pairForInvalidMapper
  ).or {
    validatables.asSequence().map { validatable ->
      findFirstFailure(right(validatable), toValidators(batchValidationConfig), throwableMapper)
        ?.mapLeft { failure -> Tuple.of(pairForInvalidMapper(validatable), failure) }
    }.firstOrNull { it?.isLeft == true }.toFailureWithPairOptional()
  }
}

@JvmSynthetic
internal fun <ContainerValidatableT, FailureT> failFastForContainer(
  containerValidationConfig: ContainerValidationConfig<ContainerValidatableT, FailureT?>,
  throwableMapper: (Throwable) -> FailureT?
): FailFastForContainer<ContainerValidatableT, FailureT> = { container: ContainerValidatableT ->
  validateBatchSize(container, containerValidationConfig).or {
    findFirstFailure(
      right(container),
      containerValidationConfig.containerValidators,
      throwableMapper
    ).toFailureOptional()
  }
}

@JvmSynthetic
internal fun <ContainerValidatableT, NestedContainerValidatableT, FailureT> failFastForContainer(
  containerValidationConfig: ContainerValidationConfigWith2Levels<ContainerValidatableT, NestedContainerValidatableT, FailureT?>,
  throwableMapper: (Throwable) -> FailureT?
): FailFastForContainer<ContainerValidatableT, FailureT> = { container: ContainerValidatableT ->
  validateBatchSize(container, containerValidationConfig)
    .or {
      findFirstFailure(
        right(container),
        containerValidationConfig.containerValidators,
        throwableMapper
      )
        .toFailureOptional()
    }
}