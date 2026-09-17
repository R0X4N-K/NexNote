package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.domain.usecase.ChangeVaultPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ClearVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ConfigureVaultPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DecryptVaultImageBytesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteVaultNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateVaultNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultAndroidCredentialAvailabilityUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.LockVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveVaultNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultTrashedNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RefreshVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RemoveNoteFromVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ResetVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreVaultNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleVaultNotePinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithAndroidCredentialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UpdateVaultNoteCreationDateUseCase

internal class VaultUseCases internal constructor(
    vaultRepository: VaultRepository,
    vaultAndroidCredentialRepository: VaultAndroidCredentialRepository,
    vaultNoteRepository: VaultNoteRepository
) {
    val observeVaultState = ObserveVaultStateUseCase(vaultRepository)
    val configureVaultPin = ConfigureVaultPinUseCase(vaultRepository)
    val unlockVaultWithPin = UnlockVaultWithPinUseCase(vaultRepository)
    val unlockVaultWithAndroidCredential =
        UnlockVaultWithAndroidCredentialUseCase(vaultRepository)
    val observeVaultAndroidCredentialProtectedMaterial =
        ObserveVaultAndroidCredentialProtectedMaterialUseCase(vaultRepository)
    val refreshVaultAndroidCredentialProtectedMaterial =
        RefreshVaultAndroidCredentialProtectedMaterialUseCase(vaultRepository)
    val clearVaultAndroidCredentialProtectedMaterial =
        ClearVaultAndroidCredentialProtectedMaterialUseCase(vaultRepository)
    val changeVaultPin = ChangeVaultPinUseCase(vaultRepository)
    val resetVault = ResetVaultUseCase(vaultRepository)
    val getVaultAndroidCredentialAvailability =
        GetVaultAndroidCredentialAvailabilityUseCase(vaultAndroidCredentialRepository)
    val lockVault = LockVaultUseCase(vaultRepository)
    val decryptVaultImageBytes = DecryptVaultImageBytesUseCase(vaultNoteRepository)
    val moveNoteToVault = MoveNoteToVaultUseCase(vaultNoteRepository)
    val moveVaultNoteToTrash = MoveVaultNoteToTrashUseCase(vaultNoteRepository)
    val restoreVaultNoteFromTrash = RestoreVaultNoteFromTrashUseCase(vaultNoteRepository)
    val deleteVaultNotePermanently = DeleteVaultNotePermanentlyUseCase(vaultNoteRepository)
    val observeVaultNotes = ObserveVaultNotesUseCase(vaultNoteRepository)
    val observeVaultNoteLinkCandidates =
        ObserveVaultNoteLinkCandidatesUseCase(vaultNoteRepository)
    val observeVaultTrashedNotes = ObserveVaultTrashedNotesUseCase(vaultNoteRepository)
    val getVaultNoteById = GetVaultNoteByIdUseCase(vaultNoteRepository)
    val saveVaultNote = SaveVaultNoteUseCase(vaultNoteRepository)
    val duplicateVaultNote = DuplicateVaultNoteUseCase(vaultNoteRepository)
    val toggleVaultNotePin = ToggleVaultNotePinUseCase(vaultNoteRepository)
    val removeNoteFromVault = RemoveNoteFromVaultUseCase(vaultNoteRepository)
    val updateVaultNoteCreationDate = UpdateVaultNoteCreationDateUseCase(vaultNoteRepository)
}
