import androidx.paging.PagingSource;
import androidx.room.paging.util.RoomPagingUtil;
import java.util.*;
public class RoomOffsetProbe {
 public static void main(String[] args) {
  int checks=0;
  for(int total:new int[]{0,1,4,49,50,51,80,125,150,160,250,43569}) {
   Set<Integer> seen=new HashSet<>();
   for(int cursor=0;cursor<total;cursor+=50) {
    var p=new PagingSource.LoadParams.Append<Integer>(cursor,50,true);
    int limit=RoomPagingUtil.getLimit(p,cursor);
    int offset=RoomPagingUtil.getOffset(p,cursor,total);
    if(offset!=cursor)throw new AssertionError("Shifted offset "+cursor+" to "+offset);
    for(int i=offset;i<Math.min(total,offset+limit);i++)if(!seen.add(i))throw new AssertionError("Repeated "+i);
   }
   if(seen.size()!=total)throw new AssertionError("Missing rows "+total);
   checks++;
  }
  var old=new PagingSource.LoadParams.Refresh<Integer>(150,50,true);
  if(RoomPagingUtil.getOffset(old,150,160)!=110)throw new AssertionError("Legacy failure not reproduced");
  System.out.println("PASS: real Room/Paging offset utility, 12 catalogue sizes without repeats or gaps; original Refresh(150) reproduces backwards shift to 110. SQL/instrumentation not executed on JVM.");
 }
}
