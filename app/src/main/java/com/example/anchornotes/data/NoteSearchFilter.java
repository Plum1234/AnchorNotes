package com.example.anchornotes.data;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

public class NoteSearchFilter implements Parcelable {
    public String query;              // nullable
    public List<Long> tagIds;         // empty = no tag filter
    public Long fromDate;             // nullable
    public Long toDate;               // nullable
    public Boolean hasPhoto;          // nullable
    public Boolean hasVoice;          // nullable
    public Boolean hasLocation;       // nullable

    public NoteSearchFilter() {
        this.tagIds = new ArrayList<>();
    }

    protected NoteSearchFilter(Parcel in) {
        query = in.readString();
        int n = in.readInt();
        tagIds = new ArrayList<>();
        for (int i=0;i<n;i++) tagIds.add(in.readLong());
        fromDate = in.readByte()==0 ? null : in.readLong();
        toDate   = in.readByte()==0 ? null : in.readLong();
        hasPhoto = (Boolean) (in.readByte()==2 ? null : (in.readByte()==1));
        hasVoice = (Boolean) (in.readByte()==2 ? null : (in.readByte()==1));
        hasLocation = (Boolean) (in.readByte()==2 ? null : (in.readByte()==1));
    }

    public static final Creator<NoteSearchFilter> CREATOR = new Creator<NoteSearchFilter>() {
        @Override public NoteSearchFilter createFromParcel(Parcel in) { return new NoteSearchFilter(in); }
        @Override public NoteSearchFilter[] newArray(int size) { return new NoteSearchFilter[size]; }
    };

    @Override public int describeContents() { return 0; }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(query);
        dest.writeInt(tagIds==null?0:tagIds.size());
        if (tagIds!=null) for (Long id: tagIds) dest.writeLong(id);
        if (fromDate==null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeLong(fromDate); }
        if (toDate==null) dest.writeByte((byte)0); else { dest.writeByte((byte)1); dest.writeLong(toDate); }
        writeNullableBool(dest, hasPhoto);
        writeNullableBool(dest, hasVoice);
        writeNullableBool(dest, hasLocation);
    }

    private void writeNullableBool(Parcel dest, Boolean b) {
        if (b==null) { dest.writeByte((byte)2); }
        else { dest.writeByte((byte)(b?1:0)); }
    }
}
