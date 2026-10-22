package br.com.flaviotorres.iptvpremium.data.cache
import br.com.flaviotorres.iptvpremium.data.parser.XtreamArrayStream
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader
class ImportBatchDeviceTest {
 @Test fun everyProfilePreservesRecordsAndUsesItsDefaultBatch()=runBlocking {
  val original=ImportTuningState.profile
  try{for(profile in ImportProfile.entries){ImportTuningState.profile=profile;val batches=mutableListOf<Int>();val raw=(1..5001).joinToString(",","[","]"){"{\"stream_id\":$it}"};val stats=XtreamArrayStream.read(StringReader(raw),"get_vod_streams"){_,count->batches.add(count)};assertEquals(5001L,stats.records);assertEquals(5001,batches.sum());assertEquals(profile.batchSize,batches.first());assertTrue(batches.all{it<=profile.batchSize})}}finally{ImportTuningState.profile=original}
 }
 @Test fun largeRecordsTriggerTextThresholdBeforeRowThreshold()=runBlocking {
  val original=ImportTuningState.profile
  try{ImportTuningState.profile=ImportProfile.LOTES;val batches=mutableListOf<Int>();val record="{\"name\":\""+"x".repeat(400000)+"\"}";XtreamArrayStream.read(StringReader(List(8){record}.joinToString(",","[","]")),"get_vod_streams"){_,n->batches.add(n)};assertEquals(8,batches.sum());assertTrue(batches.first()<ImportProfile.LOTES.batchSize);assertTrue(batches.size>1)}finally{ImportTuningState.profile=original}
 }
 @Test fun cancellationStopsProductionAfterCurrentBatch()=runBlocking {
  var emissions=0;val task=launch{XtreamArrayStream.read(StringReader((1..5000).joinToString(",","[","]"){"{\"stream_id\":$it}"}),"get_vod_streams",batchSize=10){_,_->emissions++;currentCoroutineContext().cancel()}};task.join();assertTrue(task.isCancelled);assertEquals(1,emissions)
 }
}
