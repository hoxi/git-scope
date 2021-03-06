package implementation;

import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.popup.JBPopup;
//import com.intellij.openapi.ui.popup.JBPopupFactory;
//import java.awt.*;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import listener.ChangeActionNotifierInterface;
import state.State;
import git4idea.repo.GitRepository;
import implementation.compare.MyGitCompareWithBranchAction;
import implementation.lineStatusTracker.MyLineStatusTrackerImpl;
import implementation.scope.MyScope;
import org.jetbrains.annotations.NotNull;
import state.TargetBranch;
import ui.ToolWindowUI;
import implementation.targetBranchWidget.MyBranchAction;
import utils.Git;

import javax.swing.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.joining;

public class Manager {

    private ToolWindowUI toolWindowUI;
    private MyScope myScope;
    private MyLineStatusTrackerImpl myLineStatusTrackerImpl;

    private Project project;

    private Git git;

    private boolean initialized = false;

//    Map<String, String> repositoryTargetBranchMap = null;

    private Collection<Change> changes;
    private Collection<Change> changesBefore;
    private Map<GitRepository, Collection<Change>> changesByRepoMap = new HashMap<>();

    private State state;
    private TargetBranch targetBranch;

    private Queue<String> queue;
    private MyGitCompareWithBranchAction myGitCompareWithBranchAction;
    private static boolean busy;

    private MessageBus messageBus;
    private MessageBusConnection messageBusConnection;

    private Object lastOpenedAt;

//    private InitialUpdate initialUpdate;

    public Manager() {
        // Keep Constructor empty, because this is a Service
    }

    public void init(Project project) {

        this.state = State.getInstance(project);

        this.project = project;

        this.git = new Git(project);

        this.targetBranch = new TargetBranch(project, state, git, toolWindowUI);

        // Scope
        this.myScope = new MyScope(project);

        // LST
        this.myLineStatusTrackerImpl = new MyLineStatusTrackerImpl(project);

        this.myGitCompareWithBranchAction = new MyGitCompareWithBranchAction();

//        this.initialUpdate = new InitialUpdate();

        this.initQueue();

        this.messageBus = this.project.getMessageBus();
        this.messageBusConnection = messageBus.connect();

        // Init / First compare on init
        messageBusConnection.subscribe(VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED, () -> {
            onInit();
        });

        // Listener
        this.editorListener();

    }

    public void initQueue() {
        queue = new LinkedList<>();
        QueueWorker queueWorker = new QueueWorker(queue);
        queueWorker.start();
    }

    public Git getGit() {
        return this.git;
    }

    public ToolWindowUI getToolWindowUI() {
        return toolWindowUI;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void setToolWindowUI(ToolWindowUI toolWindowUI) {
        this.toolWindowUI = toolWindowUI;
    }

    public void onInit() {

        // Do Initial Compare and Update
//        System.out.println("OnInit");

//        this.targetBranch.setFeatureActive(true);

        ToolWindowUI toolWindowUI = this.getToolWindowUI();
        boolean toolWindowWasOpen = toolWindowUI != null;
        if (toolWindowWasOpen) {
//            System.out.println("avoid duplicate doCompareAndUpdate");
            return;
        }

//        SwingUtilities.invokeLater(this::doCompareAndUpdate);
        initialUpdate();

    }

    public void initialUpdate() {
        if (initialized) {
            return;
        }
//        initialUpdate.start();
        SwingUtilities.invokeLater(() -> {
            setInitialized(true);
            doCompareAndUpdate();
        });
    }

    public boolean isInitialized() {
        return initialized;
    }

//    class InitialUpdate extends Thread {
//
//        public InitialUpdate() {
//            // ...
//        }
//
//        public void run() {
//
//            // @todo replace this hacky crap with something event based
//
//            try {
//                sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            SwingUtilities.invokeLater(() -> {
//
////                    System.out.println("InitialUpdate run");
//                    setInitialized(true);
//                    doCompareAndUpdate();
//
//                }
//            );
//        }
//
//    }

        private void editorListener() {

//        EditorFactory.getInstance().

        //        ChangeListManager.getInstance(this.project)
        //            .invokeAfterUpdate(new Runnable() {
        //                @Override
        //                public void run() {
        //                    System.out.println("OK");
        //                    doCompareAndUpdate();
        //                }
        //            },
        //            InvokeAfterUpdateMode.BACKGROUND_NOT_CANCELLABLE,
        //            "",
        //            ModalityState.NON_MODAL
        //        );



        //
        //        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
        //            public void contentsChanged(@NotNull VirtualFileEvent event) {
        //                System.out.println("contentsChanged");
        //            }
        //        }, this.project);

        //        messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {

        messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            //            @Override
            //            public void before(@NotNull List<? extends VFileEvent> events) {
            //                System.out.println("::::: before");
            //            }

            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
//                System.out.println("::::: doCompareAndUpdate after VFS_CHANGES");
                doCompareAndUpdate();
            }
        });

        //        messageBusConnection.subscribe(ChangeListManagerImpl.LISTS_LOADED, lists -> {
        //            if (lists.isEmpty()) return;
        //            try {
        //                //                doCompareAndUpdate();
        //                // ChangeListManager.getInstance(myProject).setReadOnly(LocalChangeList.DEFAULT_NAME, true);
        //
        //                //                if (!myConfiguration.changeListsSynchronized()) {
        //                //                    processChangeLists(lists);
        //                //                }
        //            } catch (ProcessCanceledException e) {
        //                //
        //            }
        //
        //        });


        messageBusConnection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile vFile) {
//                    System.out.println("fileOpened doCompareAndUpdate");
                    doCompareAndUpdate();
                }

                @Override
                public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                    //                    System.out.println("2");
                    //                    doCompareAndUpdate();
                }
            }
        );

//                VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
//                    @Override
//                    public void contentsChanged(@NotNull VirtualFileEvent event) {
//                        System.out.println("fileStatusChangedVirtualFile");
//
//                    }
//                });

        FileStatusManager.getInstance(this.project).addFileStatusListener(new FileStatusListener() {
            //            @Override
            //            public void fileStatusesChanged() {
            //                System.out.println("fileStatusesChanged");
            //                doCompareAndUpdate();
            //            }

            @Override
            public void fileStatusChanged(@NotNull VirtualFile virtualFile) {
//                System.out.println("fileStatusChangedVirtualFile");
                doCompareAndUpdate(virtualFile);
            }
        });

    }

    public void initOnOpenToolWindow() {
        targetBranch.initOnOpenToolWindow();
    }

    public String getTargetBranchDisplay() {
        return targetBranch.getTargetBranchDisplay();
    }

    public String getTargetBranchByRepository(GitRepository repo) {
        return targetBranch.getTargetBranchByRepository(repo);
    }

    public String getTargetBranchByRepositoryDisplay(GitRepository repo) {
        return targetBranch.getTargetBranchByRepositoryDisplay(repo);
    }

    public void targetBranchListener(MyBranchAction myBranchAction) {
//        System.out.println("doCompareAndUpdate targetBranchListener");
        targetBranch.targetBranchListener(myBranchAction, aVoid -> doCompareAndUpdate());
    }

    public void doCompareAndUpdate(@NotNull VirtualFile virtualFile) {

        if (!this.initialized) {
            return;
        }

        if (!this.targetBranch.isFeatureActive()) {
            // updateFromHEAD(virtualFile); //@todo
            updateFromHEAD();
            return;
        }

    }

    public void doCompareAndUpdate() {

        if (!this.initialized) {
            return;
        }

        if (!this.targetBranch.isFeatureActive()) {
            updateFromHEAD();
            return;
        }

        // System.out.println("+++");

        queue.add(""); // No parameter needed

    }

    public void setLastOpenedAt(Object lastOpenedAt) {
        this.lastOpenedAt = lastOpenedAt;
    }

    public Object getLastOpenedAt() {
        return this.lastOpenedAt;
    }

    class QueueWorker extends Thread {

        private final Queue<String> queue;
        private final Iterator iterator;
        private boolean busy;

        public QueueWorker(Queue<String> queue) {
            this.queue = queue;
            this.iterator = queue.iterator();
        }

        public void run() {

            while(true) {

                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (busy) {
                    // System.out.println("x");
                    continue;
                }
                if (!iterator.hasNext()) {
                    // System.out.println(".");
                    continue;
                }

                queue.clear();
                doJob();

            }

        }

        public void doJob() {

            busy = true;
            // System.out.println("doQueueJob");

            git.getRepositories().forEach(repo -> {
                String targetBranchByRepo = getTargetBranchByRepository(repo);
                if (targetBranchByRepo == null) {
                    // Notification.notify(Defs.NAME, "Choose a Branch");
                    toolWindowUI.showTargetBranchPopupAtToolWindow();
                    targetBranchByRepo = Git.BRANCH_HEAD;
                }

                myGitCompareWithBranchAction.collectChangesAndProcess(project, repo, targetBranchByRepo, changes -> {
                    busy = false;
                    onJobDone(repo, changes);
                });
            });

        }

    }

    public void onJobDone(GitRepository repo, Collection<Change> changes) {

        // System.out.println("done!");
        changesByRepoMap.put(repo, changes);
        update();

    }

    public void updateFromHEAD() {

//        System.out.println("updateFromHEAD");
        changes = this.getOnlyLocalChanges();
        onAfterUpdate();

    }

    //    public void updateFromHEAD(@NotNull VirtualFile virtualFile) {
    //
    //        changes = this.getOnlyLocalChanges();
    //        onAfterUpdate(virtualFile);
    //
    //    }

    public void update() {

        changes = null;

        git.getRepositories().forEach(repo -> {

            Collection<Change> changesByRepo = changesByRepoMap.get(repo);

            if (changesByRepo == null) {
                return;
            }

            if (changes == null) {
                changes = changesByRepo;
                return;
            }

            Stream<Change> combinedStream = Stream.of(changes, changesByRepo).flatMap(Collection::stream);
            Collection<Change> collectionCombined = combinedStream.collect(Collectors.toList());
            changes = collectionCombined;

        });

//        changes = addLocalChanges(changes);

        if (changes == null) {
            return;
        }

        //        if (toolWindowUI != null) {
        //            toolWindowUI.setLoadingStatus(false);
        //        }


        onAfterUpdate();
    }

    public void onAfterUpdate() {

//        System.out.println("+++ onAfterUpdate +++");

        Boolean changesAreTheSame = false;
        if (changesBefore != null && changesBefore.equals(changes)) {
            changesAreTheSame = true;
        }

        changesBefore = changes;

        this.updateToolWindowUI();

//        if (!changesAreTheSame) {
            this.updateDiff();
//        }
        this.updateLst();
        this.updateScope();

        // PUBLISH to custom listener
        ChangeActionNotifierInterface publisher = messageBus.syncPublisher(ChangeActionNotifierInterface.CHANGE_ACTION_TOPIC);
        publisher.doAction("doAction");

    }

    public void onAfterUpdate(@NotNull VirtualFile virtualFile) {

        this.updateToolWindowUI();
        this.updateDiff();
        this.updateLst(virtualFile);
        this.updateScope();

        // PUBLISH to custom listener
        ChangeActionNotifierInterface publisher = messageBus.syncPublisher(ChangeActionNotifierInterface.CHANGE_ACTION_TOPIC);
        publisher.doAction("doAction");

    }

    public void updateToolWindowUI() {

        ToolWindowUI toolWindowUI = this.getToolWindowUI();
        if (toolWindowUI != null) {
            toolWindowUI.update();
        }

    }

    public void updateDiff() {

        if (this.toolWindowUI == null) {
            return;
        }

        this.toolWindowUI.updateVcsTree(this.changes);

    }

    public void updateLst() {
        this.myLineStatusTrackerImpl.update(this.changes, null);
    }

    public void updateLst(@NotNull VirtualFile virtualFile) {

        // @todo
        // Boolean isFeatureActive = !this.targetBranch.isFeatureActive();
        this.myLineStatusTrackerImpl.update(this.changes, virtualFile);

    }

    public void updateScope() {
        this.myScope.update(this.changes);
    }

    public Collection<Change> getOnlyLocalChanges() {

//        Collection<Change> changes = new ArrayList<>();

        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> localChanges = changeListManager.getAllChanges();

//        for (Change localChange : localChanges) {
//
//            VirtualFile localChangeVirtualFile = localChange.getVirtualFile();
//            if (localChangeVirtualFile == null) {
//                continue;
//            }
//
//            changes.add(localChange);
//
//        }

        return localChanges;
    }

    public Collection<Change> addLocalChanges(Collection<Change> changes) {

//        if (changes == null) {
//            return;
//        }

        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> localChanges = changeListManager.getAllChanges();

        for (Change localChange : localChanges) {

            VirtualFile localChangeVirtualFile = localChange.getVirtualFile();
            if (localChangeVirtualFile == null) {
                continue;
            }
            String localChangePath = localChangeVirtualFile.getPath();

//            System.out.println("=================================================================");
//            System.out.println(localChangePath);
//
//            String repoPath = repo.toString();
//            System.out.println(repoPath);
//
//            if (!isLocalChangeForThisRepo(localChangePath)) {
//                System.out.println("not for this Repo - Skip!");
//                continue;
//            }

            if (isLocalChangeOnly(localChangePath, changes)) {
                changes.add(localChange);
            }

        }

        return changes;
    }
//
//    private Boolean isLocalChangeForThisRepo(String localChangePath) {
//        GitRepository repo = MyGitCompareWithBranchAction.this.repo;
//        String repoPath = repo.toString();
//        return localChangePath.contains(repoPath);
//    }

    private Boolean isLocalChangeOnly(String localChangePath, Collection<Change> changes) {

        if (changes == null) {
            return false;
        }

        for (Change change : changes) {
            VirtualFile vFile = change.getVirtualFile();
            if (vFile == null) {
                return false;
            }
            String changePath = change.getVirtualFile().getPath();

            if (localChangePath.equals(changePath)) {
                // we have already this file in our changes-list
                return false;
            }
        }

        return true;

    }

    public void toggleHeadAction() {

        this.targetBranch.toggleFeature();

//        System.out.println("doCompareAndUpdate toggleHeadAction");
        this.doCompareAndUpdate();

    }

//    @NotNull
//    JBPopup createPopup(ToolWindowUI toolWindow) {
//
//        JBPopup popup = JBPopupFactory
//                .getInstance()
//                .createComponentPopupBuilder(
//                        toolWindow.getRootPanel(),
//                        null
//                )
//                .setMinSize(new Dimension(200, 400))
//                .createPopup();
//
//        return popup;
//
//    }

}
