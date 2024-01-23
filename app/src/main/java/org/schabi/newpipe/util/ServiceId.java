package org.schabi.newpipe.util;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.services.bandcamp.BandcampService;
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCService;
import org.schabi.newpipe.extractor.services.peertube.PeertubeService;
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudService;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;

// All Services we know of.
public enum ServiceId {
    YOUTUBE,
    SOUNDCLOUD,
    MEDIA_CCC,
    PEERTUBE,
    BANDCAMP;

    public static ServiceId of(final StreamingService service) {
        if (service instanceof YoutubeService) {
            return YOUTUBE;
        } else if (service instanceof SoundcloudService) {
            return SOUNDCLOUD;
        } else if (service instanceof MediaCCCService) {
            return MEDIA_CCC;
        } else if (service instanceof PeertubeService) {
            return PEERTUBE;
        } else if (service instanceof BandcampService) {
            return BANDCAMP;
        } else {
            throw new IllegalArgumentException(
                    String.format("Service %s cannot be used in NewPipe yet.",
                            service.getServiceInfo().getName()));
        }
    }

    public StreamingService getService() {
        return switch (this) {
            case YOUTUBE -> ServiceList.YouTube;
            case SOUNDCLOUD -> ServiceList.SoundCloud;
            case MEDIA_CCC ->  ServiceList.MediaCCC;
            case PEERTUBE -> ServiceList.PeerTube;
            case BANDCAMP -> ServiceList.Bandcamp;
        };
    }
}

