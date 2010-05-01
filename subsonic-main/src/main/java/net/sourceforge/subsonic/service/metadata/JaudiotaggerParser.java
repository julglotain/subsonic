/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.service.metadata;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.MusicFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.jaudiotagger.tag.reference.GenreTypes;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses meta data from audio files using the Jaudiotagger library
 * (http://www.jthink.net/jaudiotagger/)
 *
 * @author Sindre Mehus
 */
public class JaudiotaggerParser extends MetaDataParser {

    private static final Logger LOG = Logger.getLogger(JaudiotaggerParser.class);
    private static final Pattern GENRE_PATTERN = Pattern.compile("\\((\\d+)\\).*");
    private static final Pattern TRACK_NUMBER_PATTERN = Pattern.compile("(\\d+)/\\d+");

    static {
        try {
            LogManager.getLogManager().reset();
        } catch (Throwable x) {
            LOG.warn("Failed to turn off logging from Jaudiotagger.", x);
        }
    }

    /**
     * Parses meta data for the given music file. No guessing or reformatting is done.
     *
     * @param file The music file to parse.
     * @return Meta data for the file.
     */
    @Override
    public MusicFile.MetaData getRawMetaData(MusicFile file) {

        MusicFile.MetaData metaData = getBasicMetaData(file);

        try {
            AudioFile audioFile = AudioFileIO.read(file.getFile());
            Tag tag = audioFile.getTag();
            if (tag != null) {
                metaData.setArtist(getTagField(tag, FieldKey.ARTIST));
                metaData.setAlbum(getTagField(tag, FieldKey.ALBUM));
                metaData.setTitle(getTagField(tag, FieldKey.TITLE));
                metaData.setYear(getTagField(tag, FieldKey.YEAR));
                metaData.setGenre(mapGenre(getTagField(tag, FieldKey.GENRE)));
                metaData.setDiscNumber(parseDiscNumber(getTagField(tag, FieldKey.DISC_NO)));
                metaData.setTrackNumber(parseTrackNumber(getTagField(tag, FieldKey.TRACK)));
            }

            AudioHeader audioHeader = audioFile.getAudioHeader();
            if (audioHeader != null) {
                metaData.setVariableBitRate(audioHeader.isVariableBitRate());
                metaData.setBitRate((int) audioHeader.getBitRateAsNumber());
                metaData.setDuration(audioHeader.getTrackLength());
            }


        } catch (Throwable x) {
            LOG.warn("Error when parsing tags in " + file, x);
        }

        return metaData;
    }

    private String getTagField(Tag tag, FieldKey fieldKey) {
        try {
            return StringUtils.trimToNull(tag.getFirst(fieldKey));
        } catch (Exception x) {
            // Ignored.
            return null;
        }
    }

    /**
     * Returns all tags supported by id3v1.
     */
    public static SortedSet<String> getID3V1Genres() {
        return new TreeSet<String>(GenreTypes.getInstanceOf().getAlphabeticalValueList());
    }

    /**
     * Sometimes the genre is returned as "(17)" or "(17)Rock", instead of "Rock".  This method
     * maps the genre ID to the corresponding text.
     */
    private String mapGenre(String genre) {
        if (genre == null) {
            return null;
        }
        Matcher matcher = GENRE_PATTERN.matcher(genre);
        if (matcher.matches()) {
            int genreId = Integer.parseInt(matcher.group(1));
            if (genreId >= 0 && genreId < GenreTypes.getInstanceOf().getSize()) {
                return GenreTypes.getInstanceOf().getValueForId(genreId);
            }
        }
        return genre;
    }

    /**
     * Parses the track number from the given string.  Also supports
     * track numbers on the form "4/12".
     */
    private Integer parseTrackNumber(String trackNumber) {
        if (trackNumber == null) {
            return null;
        }

        Integer result = null;

        try {
            result = new Integer(trackNumber);
        } catch (NumberFormatException x) {
            Matcher matcher = TRACK_NUMBER_PATTERN.matcher(trackNumber);
            if (matcher.matches()) {
                try {
                    result = Integer.valueOf(matcher.group(1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        if (Integer.valueOf(0).equals(result)) {
            return null;
        }
        return result;
    }

    private Integer parseDiscNumber(String discNumber) {
        if (discNumber == null) {
            return null;
        }
        try {
            return Integer.valueOf(discNumber);
        } catch (NumberFormatException x) {
            return null;
        }
    }

    /**
    * Updates the given file with the given meta data.
    *
    * @param file     The music file to update.
    * @param metaData The new meta data.
    */
    @Override
    public void setMetaData(MusicFile file, MusicFile.MetaData metaData) {

        try {
            AudioFile audioFile = AudioFileIO.read(file.getFile());
            Tag tag = audioFile.getTagOrCreateAndSetDefault();

            tag.setField(FieldKey.ARTIST, StringUtils.trimToEmpty(metaData.getArtist()));
            tag.setField(FieldKey.ALBUM, StringUtils.trimToEmpty(metaData.getAlbum()));
            tag.setField(FieldKey.TITLE, StringUtils.trimToEmpty(metaData.getTitle()));
            tag.setField(FieldKey.YEAR, StringUtils.trimToEmpty(metaData.getYear()));
            tag.setField(FieldKey.GENRE, StringUtils.trimToEmpty(metaData.getGenre()));

            Integer track = metaData.getTrackNumber();
            if (track == null) {
                tag.deleteField(FieldKey.TRACK);
            } else {
                tag.setField(FieldKey.TRACK, String.valueOf(track));
            }

            audioFile.commit();

        } catch (Throwable x) {
            LOG.warn("Failed to update tags for file " + file, x);
            throw new RuntimeException("Failed to update tags for file " + file + ". " + x.getMessage(), x);
        }
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always true.
     */
    @Override
    public boolean isEditingSupported() {
        return true;
    }

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param file The music file in question.
     * @return Whether this parser is applicable to the given file.
     */
    @Override
    public boolean isApplicable(MusicFile file) {
        if (!file.isFile()) {
            return false;
        }

        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();

        return extension.equals("mp3") ||
               extension.equals("m4a") ||
               extension.equals("aac") ||
               extension.equals("ogg") ||
               extension.equals("flac") ||
               extension.equals("wav") ||
               extension.equals("mpc") ||
               extension.equals("mp+") ||
               extension.equals("ape") ||
               extension.equals("wma");
    }

    /**
     * Returns whether cover art image data is available in the given file.
     *
     * @param file The music file.
     * @return Whether cover art image data is available.
     */
    public boolean isImageAvailable(MusicFile file) {
        try {
            return getArtwork(file) != null;
        } catch (Throwable x) {
            LOG.warn("Failed to find cover art tag in " + file, x);
            return false;
        }
    }

    /**
     * Returns the cover art image data embedded in the given file.
     *
     * @param file The music file.
     * @return The embedded cover art image data, or <code>null</code> if not available.
     */
    public byte[] getImageData(MusicFile file) {
        try {
            return getArtwork(file).getBinaryData();
        } catch (Throwable x) {
            LOG.warn("Failed to find cover art tag in " + file, x);
            return null;
        }
    }

    private Artwork getArtwork(MusicFile file) throws Exception {
        AudioFile audioFile = AudioFileIO.read(file.getFile());
        Tag tag = audioFile.getTag();
        return tag == null ? null : tag.getFirstArtwork();
    }
}