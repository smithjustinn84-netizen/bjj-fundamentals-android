package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.testing.StorageContract

class MemoryStorageContractTest : StorageContract() {
    override suspend fun create(initialData: BackupData): AppStorage = MemoryStorage(initialData)
}
