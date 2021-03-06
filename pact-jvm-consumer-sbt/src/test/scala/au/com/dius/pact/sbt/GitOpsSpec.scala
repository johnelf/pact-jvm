package au.com.dius.pact.sbt

import org.specs2.mutable.Specification
import java.io.{FileWriter, File}
import sbt.{IO, Logger}
import org.specs2.mock.Mockito
import com.typesafe.sbt.git.ConsoleGitRunner
import scala.annotation.tailrec

class GitOpsSpec extends Specification with Mockito {

  def writeToFile(file: File, s:String) {
    val fw = new FileWriter(file)
    try {
      fw.write(s)
    } finally {
      fw.close()
    }
  }

  "push pacts" should {
    sequential


    def setup(deriveFile: (File, File) => File) = {
      val log = mock[Logger]
      val dryRun = true

      val testPactFile = new File(this.getClass.getClassLoader.getResource("pacts/testpact.txt").getFile)

      @tailrec
      def findTarget(f:File):File = {
        if(f.getName == "target") f else findTarget(new File(f.getParent))
      }

      val targetDir = findTarget(testPactFile)

      val repoDirName = "repo/testRepo"
      val repoDir = new File(targetDir, repoDirName)

      IO.delete(repoDir)

      val repoUrl = repoDir.getAbsolutePath
      IO.createDirectory(repoDir)
      val providerPactDir = "pacts"
      val pactDir = new File(repoDir, providerPactDir)
      IO.createDirectory(pactDir)
      IO.copyFile(testPactFile, new File(pactDir, testPactFile.getName))

      ConsoleGitRunner("init")(repoDir)
      ConsoleGitRunner("add", ".")(repoDir)
      ConsoleGitRunner("commit", "-am", "init")(repoDir)

      GitOps.pushPact(deriveFile(targetDir, testPactFile), providerPactDir, repoUrl, targetDir, log, dryRun)
    }

    "short circuit for no changes" in {
      val result = setup((targetDir, testPactFile) => testPactFile)

      result must beEqualTo(TerminatingResult("nothing to commit"))
    }

    "work properly when there are changes" in {
      val result = setup((targetDir, testPactFile) => {
        val modifiedPactFile = new File(targetDir, "modifiedPact.txt")
        IO.copyFile(testPactFile, modifiedPactFile)
        writeToFile(modifiedPactFile, "something new")
        modifiedPactFile
      })

      result must beEqualTo(HappyResult("Pact committed without pushing"))
    }
  }
}
