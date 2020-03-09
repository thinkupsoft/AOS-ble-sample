package no.nordicsemi.android.meshprovisioner;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MeshDataConverter {

    @TypeConverter
    public  List<Integer> stringToListNumbers(String data) {
         if (data == null) {
             return new ArrayList<Integer>();
        } else {
             Gson gson = new Gson();
             Type listType = new TypeToken<ArrayList<Integer>>(){}.getType();

             return gson.fromJson(data, listType);
         }
    }

    @TypeConverter
    public String listToStringNumbers(List<Integer> numbers){
         return new Gson().toJson(numbers);
    }
}
