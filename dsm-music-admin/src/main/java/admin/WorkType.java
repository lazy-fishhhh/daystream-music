package admin;

import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;



@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum WorkType {
    MUSIC("歌曲", 1),
    MV("MV" , 2);

     String name;
     Integer value;

     public static Integer getCodeByName(String name){
         for (WorkType workType : WorkType.values()){
             if (workType.name.equals(name)){
                 return workType.value;
             }
         }
         return null;
     }

     public static String getNameByCode(Integer code){
         for (WorkType workType : WorkType.values()){
             if (workType.value.equals(code)){
                 return workType.name;
             }
         }
         return null;
     }
}
