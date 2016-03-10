package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implements SORT command described in <a href="https://tools.ietf.org/html/rfc5256">RFC5256</a>
 * <br><br>
 * Created on 10/03/2016.
 *
 * @author Reda.Housni-Alaoui
 */
class SortCommand extends SelectedStateCommand implements UidEnabledCommand {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    public static final String NAME = "SORT";
    public static final String ARGS = "(<sort criteria>) <charset specification> <search term>";

    private SortCommandParser parser = new SortCommandParser();

    SortCommand() {
        super(NAME, ARGS);
    }

    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException, AuthorizationException {
        doProcess(request, response, session, false);
    }

    @Override
    public void doProcess(final ImapRequestLineReader request,
                          ImapResponse response,
                          ImapSession session,
                          boolean useUids) throws ProtocolException, FolderException {
        final SortTerm sortTerm = parser.sortTerm(request);

        final MailFolder folder = session.getSelected();

        long[] uids = folder.search(sortTerm.getSearchTerm());
        List<StoredMessage> messages = new ArrayList<>();
        for (long uid : uids) {
            messages.add(folder.getMessage(uid));
        }

        Collections.sort(messages, new StoredMessageSorter(sortTerm));

        StringBuilder idList = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                idList.append(SP);
            }
            StoredMessage message = messages.get(i);
            if (useUids) {
                idList.append(message.getUid());
            } else {
                int msn = folder.getMsn(message.getUid());
                idList.append(msn);
            }
        }

        response.commandResponse(this, idList.toString());

        boolean omitExpunged = !useUids;
        session.unsolicitedResponses(response, omitExpunged);
        response.commandComplete(this);
    }

}
