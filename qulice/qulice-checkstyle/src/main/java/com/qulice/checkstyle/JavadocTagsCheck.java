/**
 * Copyright (c) 2011, Qulice.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the Qulice.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.qulice.checkstyle;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Check if the class/interface javadoc contains properly formatted author
 * and version tags.
 *
 * @author Krzysztof Krason (Krzysztof.Krason@gmail.com)
 * @author Yegor Bugayenko (yegor@qulice.com)
 * @version $Id$
 */
public final class JavadocTagsCheck extends Check {

    /**
     * Map of tag and its pattern.
     */
    private final Map<String, Pattern> tags = new HashMap<String, Pattern>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        this.tags.put(
            "author",
            // @checkstyle LineLength (1 line)
            Pattern.compile("^([A-Z](\\.|[a-z]+) ){2,}\\([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}\\)$")
        );
        this.tags.put("version", Pattern.compile("^\\$Id.*\\$$"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{
            TokenTypes.CLASS_DEF,
            TokenTypes.INTERFACE_DEF,
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitToken(final DetailAST ast) {
        if (ast.getParent() == null) {
            final String[] lines = this.getLines();
            final int start = ast.getLineNo();
            final int commentStart = this.findCommentStart(lines, start);
            final int commentEnd = this.findCommentEnd(lines, start);
            if ((commentEnd > commentStart) && (commentStart >= 0)) {
                for (String tag : this.tags.keySet()) {
                    this.matchTagFormat(lines, commentStart, commentEnd, tag);
                }
            } else {
                this.log(0, "Problem finding class/interface comment");
            }
        }
    }

    /**
     * Check if the tag text matches the format from pattern.
     * @param lines List of all lines.
     * @param start Line number where comment starts.
     * @param end Line number where comment ends.
     * @param tag Name of the tag.
     * @checkstyle ParameterNumber (3 lines)
     */
    private void matchTagFormat(final String[] lines, final int start,
        final int end, final String tag) {
        final int line = this.findTagLineNum(lines, start, end, tag);
        if (line == -1) {
            this.log(
                start + 1,
                "Missing ''@{0}'' tag in class/interface comment",
                tag
            );
            return;
        }
        final String text = this.getTagText(lines[line]);
        if (!this.tags.get(tag).matcher(text).matches()) {
            this.log(
                line + 1,
                "Tag text ''{0}'' does not match the pattern ''{1}''",
                text,
                this.tags.get(tag).toString()
            );
        }
    }

    /**
     * Get the text of the given tag.
     * @param line Line with the tag.
     * @return The text of the tag.
     */
    private String getTagText(final String line) {
        return line.substring(
            line.indexOf(" ", line.indexOf("@")) + 1
        );
    }

    /**
     * Find given tag in comment lines.
     * @param lines Lines to search for the tag.
     * @param start Starting line number.
     * @param end Ending line number.
     * @param tag Name of the tag to look for.
     * @return Line number with found tag or -1 otherwise.
     * @checkstyle ParameterNumber (3 lines)
     */
    private int findTagLineNum(final String[] lines, final int start,
        final int end, final String tag) {
        final String prefix = String.format(" * @%s ", tag);
        int found = -1;
        for (int pos = start; pos <= end; pos += 1) {
            final String line = lines[pos];
            if (line.contains(String.format("@%s ", tag))) {
                if (!line.startsWith(prefix)) {
                    this.log(
                        start + pos + 1,
                        "Line with ''@{0}'' does not start with a ''{1}''",
                        tag,
                        prefix
                    );
                    found = -1;
                    break;
                }
                found = pos;
                break;
            }
        }
        return found;
    }

    /**
     * Find javadoc starting comment.
     * @param lines List of lines to check.
     * @param start Start searching from this line number.
     * @return Line number with found starting comment or -1 otherwise.
     */
    private int findCommentStart(final String[] lines, final int start) {
        return this.findTrimmedTextUp(lines, start, "/**");
    }

    /**
     * Find javadoc ending comment.
     * @param lines List of lines to check.
     * @param start Start searching from this line number.
     * @return Line number with found ending comment, or -1 if it wasn't found.
     */
    private int findCommentEnd(final String[] lines, final int start) {
        return this.findTrimmedTextUp(lines, start, "*/");
    }

    /**
     * Find a text in lines, by going up.
     * @param lines List of lines to check.
     * @param start Start searching from this line number.
     * @param text Text to find.
     * @return Line number with found text, or -1 if it wasn't found.
     */
    private int findTrimmedTextUp(final String[] lines,
        final int start, final String text) {
        int found = -1;
        for (int pos = start - 1; pos >= 0; pos -= 1) {
            if (lines[pos].trim().equals(text)) {
                found = pos;
                break;
            }
        }
        return found;
    }

}

