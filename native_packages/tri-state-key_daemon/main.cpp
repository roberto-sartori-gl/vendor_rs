#include <sys/inotify.h>
#include <limits.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <dirent.h>
#include <android-base/logging.h>
#define LOG_TAG "tri-state-key_daemon"

#define BUF_LEN (10 * (sizeof(struct inotify_event) + NAME_MAX + 1))

int main()
{
    char buf[BUF_LEN];
    int inotify_fd = 0;
    struct inotify_event *event = NULL;
    char *pathname = (char *)"/dev/input/event";
    // Check for the event number related to the tri-state-key
    DIR *d;
    struct dirent *dir;
    d = opendir("/sys/bus/platform/drivers/tri-state-key/soc:tri_state_key/input");
    if (d)
    {
        while ((dir = readdir(d)) != NULL)
        {
            if (!std::isalpha(dir->d_name[0])) continue;
            char end_char = dir->d_name[strlen(dir->d_name)-1];
            char* name_with_extension;
            name_with_extension = (char *) malloc(strlen(pathname)+1);
            strcpy(name_with_extension, pathname);
            sprintf(name_with_extension, "%s%c", name_with_extension, end_char);
            pathname = name_with_extension;
        }
        closedir(d);
    }
    // 'pathname' is now the correct input device
    inotify_fd = inotify_init();
    inotify_add_watch(inotify_fd, pathname, IN_ACCESS);
    while (true)
    {
        int n = read(inotify_fd, buf, BUF_LEN);
        char *p = buf;
        while (p < buf + n)
        {
            system("am broadcast -a com.oneplus.TRI_STATE_EVENT");
            event = (struct inotify_event *)p;
            LOG(DEBUG) << "tri-state-key event detected, launching broadcast...";
            /*uint32_t mask = event->mask;
            if (mask & IN_ACCESS)
            {
                printf("File has been accessed\n");
            }
            if (mask & IN_ATTRIB)
            {
                printf("File meta data changed\n");
            }
            if (mask & IN_CLOSE_WRITE)
            {
                printf("File closed after write\n");
            }
            if (mask & IN_CLOSE_NOWRITE)
            {
                printf("File closed after read\n");
            }
            if (mask & IN_DELETE_SELF)
            {
                printf("File is deleted\n");
            }
            if (mask & IN_MODIFY)
            {
                printf("File has been modified\n");
            }
            if (mask & IN_MOVE_SELF)
            {
                printf("File has been moved\n");
            }
            if (mask & IN_OPEN)
            {
                printf("File has been opened\n");
            }
            if (mask & IN_IGNORED)
            {
                printf("File monitor has been removed\n");
            }*/
            p += sizeof(struct inotify_event) + event->len;
        }
    }
    return 0;
}