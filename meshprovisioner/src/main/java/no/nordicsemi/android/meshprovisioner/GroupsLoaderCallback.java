package no.nordicsemi.android.meshprovisioner;

import java.util.List;

public interface GroupsLoaderCallback {
    void onGroupsLoaded(List<Group> groupsList);
}
