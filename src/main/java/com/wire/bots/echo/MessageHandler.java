//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.echo;

import com.wire.blender.Blender;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.FileAsset;
import com.wire.bots.sdk.assets.FileAssetPreview;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.models.*;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.state.State;
import com.wire.bots.sdk.tools.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageHandler extends MessageHandlerBase {
    private final ConcurrentHashMap<String, Blender> blenders = new ConcurrentHashMap<>();
    private final ClientRepo repo;
    private final StorageFactory storageFactory;

    MessageHandler(ClientRepo repo, StorageFactory storageFactory) {
        this.repo = repo;
        this.storageFactory = storageFactory;
    }

    /**
     * @param newBot Initialization object for new Bot instance
     *               -  id          : The unique user ID for the bot.
     *               -  client      : The client ID for the bot.
     *               -  origin      : The profile of the user who requested the bot, as it is returned from GET /bot/users.
     *               -  conversation: The conversation as seen by the bot and as returned from GET /bot/conversation.
     *               -  token       : The bearer token that the bot must use on inbound requests.
     *               -  locale      : The preferred locale for the bot to use, in form of an IETF language tag.
     * @return If TRUE is returned new bot instance is created for this conversation
     * If FALSE is returned this service declines to create new bot instance for this conversation
     */
    @Override
    public boolean onNewBot(NewBot newBot) {
        Logger.info(String.format("onNewBot: bot: %s, username: %s",
                newBot.id,
                newBot.origin.handle));

        for (Member member : newBot.conversation.members) {
            if (member.service != null) {
                Logger.warning("Rejecting NewBot. Provider: %s service: %s",
                        member.service.provider,
                        member.service.id);
                return false; // we don't want to be in a conv if other bots are there.
            }
        }
        return true;
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            Logger.info("Received Text. bot: %s, from: %s", client.getId(), msg.getUserId());

            // send echo back to user
            client.sendText("You wrote: " + msg.getText());
        } catch (Exception e) {
            Logger.error("onText: %s", e);
        }
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        try {
            Logger.info("Received Image: type: %s, size: %,d KB, h: %d, w: %d, tag: %s",
                    msg.getMimeType(),
                    msg.getSize() / 1024,
                    msg.getHeight(),
                    msg.getWidth(),
                    msg.getTag()
            );

            // download this image from Wire server
            byte[] img = client.downloadAsset(msg.getAssetKey(),
                    msg.getAssetToken(),
                    msg.getSha256(),
                    msg.getOtrKey());

            // echo this image back to user
            client.sendPicture(img, msg.getMimeType());
        } catch (Exception e) {
            Logger.error("onImage: %s", e);
        }
    }

    @Override
    public void onAudio(WireClient client, AudioMessage msg) {
        try {
            Logger.info("Received Audio: name: %s, type: %s, size: %,d KB, duration: %,d sec",
                    msg.getName(),
                    msg.getMimeType(),
                    msg.getSize() / 1024,
                    msg.getDuration() / 1000
            );

            // download this audio from Wire Server
            byte[] audio = client.downloadAsset(msg.getAssetKey(),
                    msg.getAssetToken(),
                    msg.getSha256(),
                    msg.getOtrKey());

            // echo this audio back to user
            client.sendAudio(audio,
                    msg.getName(),
                    msg.getMimeType(),
                    msg.getDuration());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onVideo(WireClient client, VideoMessage msg) {
        try {
            Logger.info("Received Video: name: %s, type: %s, size: %,d KB, duration: %,d sec",
                    msg.getName(),
                    msg.getMimeType(),
                    msg.getSize() / 1024,
                    msg.getDuration() / 1000
            );

            // download this video from Wire Server
            byte[] video = client.downloadAsset(msg.getAssetKey(),
                    msg.getAssetToken(),
                    msg.getSha256(),
                    msg.getOtrKey());

            // echo this video back to user
            client.sendVideo(video,
                    msg.getName(),
                    msg.getMimeType(),
                    msg.getDuration(),
                    msg.getHeight(),
                    msg.getWidth());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttachment(WireClient client, AttachmentMessage attach) {
        try {
//            // save it locally
//            File file = new File(attach.getName());
//            try (FileOutputStream fos = new FileOutputStream(file)) {
//                // download file from Wire servers
//                byte[] bytes = client.downloadAsset(
//                        attach.getAssetKey(),
//                        attach.getAssetToken(),
//                        attach.getSha256(),
//                        attach.getOtrKey());
//
//                fos.write(bytes);
//
//                Logger.info("Received Attachment: name: %s, type: %s, size: %,d KB",
//                        attach.getName(),
//                        attach.getMimeType(),
//                        bytes.length / 1024
//                );
//            }
//
//            // echo this file back to user
//            client.sendFile(file, attach.getMimeType());
//
//            if (!file.delete())
//                Logger.warning("Failed to delete file: %s", file.getPath());

            Logger.info("Received Attachment: name: %s, type: %s, size: %,d KB",
                    attach.getName(),
                    attach.getMimeType(),
                    attach.getSize() / 1024
            );

            // echo this file back to user
            String messageId = UUID.randomUUID().toString();
            FileAssetPreview preview = new FileAssetPreview(attach.getName(), attach.getMimeType(), attach.getSize(), messageId);
            FileAsset asset = new FileAsset(attach.getAssetKey(), attach.getAssetToken(), attach.getSha256(), messageId);

            client.sendDirectFile(preview, asset, attach.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            Logger.info("onNewConversation: bot: %s, conv: %s",
                    client.getId(),
                    client.getConversationId());

            String label = "Hello! I am Echo. I echo everything you write";
            client.sendText(label);
        } catch (Exception e) {
            Logger.error("onNewConversation: %s", e);
        }
    }

    @Override
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
        try {
            Collection<User> users = client.getUsers(userIds);
            for (User user : users) {
                Logger.info("onMemberJoin: bot: %s, user: %s/%s @%s",
                        client.getId(),
                        user.id,
                        user.name,
                        user.handle
                );

                // say Hi to new participant
                client.sendText("Hi there " + user.name);
            }
        } catch (Exception e) {
            Logger.error("onMemberJoin: %s", e);
        }
    }

    @Override
    public void onMemberLeave(WireClient client, ArrayList<String> userIds) {
        Logger.info("onMemberLeave: users: %s, bot: %s",
                userIds,
                client.getId());
    }

    @Override
    public void onBotRemoved(String botId) {
        Logger.info("Bot: %s got removed from the conversation :(", botId);
    }

    @Override
    public void onCalling(WireClient client, String userId, String clientId, String content) {
        String botId = client.getId();
        Blender blender = getBlender(botId);
        blender.recvMessage(botId, userId, clientId, content);
    }

    private Blender getBlender(String botId) {
        return blenders.computeIfAbsent(botId, k -> {
            try {
                String module = Service.CONFIG.getModule();
                String ingress = Service.CONFIG.getIngress();
                int portMin = Service.CONFIG.getPortMin();
                int portMax = Service.CONFIG.getPortMax();

                State state = storageFactory.create(botId);
                NewBot bot = state.getState();
                Blender blender = new Blender();
                blender.init(module, botId, bot.client, ingress, portMin, portMax);
                blender.registerListener(new CallListener(repo));
                return blender;
            } catch (Exception e) {
                Logger.error(e.toString());
                return null;
            }
        });
    }
}
