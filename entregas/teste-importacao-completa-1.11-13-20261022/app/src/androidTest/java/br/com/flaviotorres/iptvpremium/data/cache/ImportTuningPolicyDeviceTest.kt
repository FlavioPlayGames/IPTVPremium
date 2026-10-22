package br.com.flaviotorres.iptvpremium.data.cache
import org.junit.Assert.*
import org.junit.Test
class ImportTuningPolicyDeviceTest {
 @Test fun releaseIgnoresEverySavedExperiment(){for(p in ImportProfile.entries)assertEquals(ImportProfile.PADRAO,ImportProfile.stored(p.name,false))}
 @Test fun invalidPreferencesKeepValidatedDefaults(){assertEquals(ImportProfile.PADRAO,ImportProfile.stored("bad",true));assertEquals(ImportProfile.PADRAO,ImportProfile.stored(null,true))}
 @Test fun individualProfilesChangeOnlyIntendedSettings(){assertFalse(ImportProfile.ATUAL.forceWal);assertTrue(ImportProfile.WAL.forceWal);assertFalse(ImportProfile.WAL.normal);assertEquals(250,ImportProfile.WAL.batchSize);assertTrue(ImportProfile.NORMAL.normal);assertEquals(250,ImportProfile.NORMAL.batchSize);assertEquals(2000,ImportProfile.LOTES.batchSize);assertEquals(2*1024*1024,ImportProfile.LOTES.batchCharacters)}
 @Test fun combinedProfilesKeepM3uAt2000WithoutForcingJournalChanges(){for(p in listOf(ImportProfile.XTREAM_1000,ImportProfile.XTREAM_500,ImportProfile.XTREAM_650)){assertEquals(2000,p.m3uBatchSize);assertEquals(2*1024*1024,p.m3uBatchCharacters);assertFalse(p.forceWal);assertFalse(p.normal)};assertEquals(1000,ImportProfile.XTREAM_1000.batchSize);assertEquals(500,ImportProfile.XTREAM_500.batchSize);assertEquals(650,ImportProfile.XTREAM_650.batchSize);assertEquals(ImportProfile.XTREAM_500.batchCharacters,ImportProfile.XTREAM_650.batchCharacters)}
 @Test fun productionDefaultsAre250And2000(){assertEquals(ImportProfile.PADRAO,ImportProfile.DEFAULT);assertEquals(250,ImportProfile.DEFAULT.batchSize);assertEquals(2000,ImportProfile.DEFAULT.m3uBatchSize);assertFalse(ImportProfile.DEFAULT.forceWal);assertFalse(ImportProfile.DEFAULT.normal)}
 @Test fun invalidRequestedModeDoesNotSilentlySelectAnExperiment(){try{ImportProfile.requested("bad");fail()}catch(expected:IllegalArgumentException){}}
}
